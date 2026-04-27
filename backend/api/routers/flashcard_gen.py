import hashlib
import json
import httpx
import time
import traceback
from typing import Optional, AsyncGenerator
from fastapi import APIRouter, UploadFile, File, HTTPException, Form, Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from backend.services.llm import get_llm_client
from backend.services.llm import factory as llm_factory
from backend.services.rag.components.parsers.pdf import PDFParser
from backend.services.language_detection import detect_language, get_language_name
from backend.api.rate_limiter import limiter

router = APIRouter()

MAX_TEXT_LENGTH = 200_000
_CACHE_TTL = 3600  # seconds

# in-memory cache: md5(text+count+difficulty) -> {"flashcards": [...], "ts": float}
_flashcard_cache: dict[str, dict] = {}


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class GenerateFlashcardsResponse(BaseModel):
    flashcards: list[dict[str, str]]
    language: str = "en"
    language_name: str = "English"


class ExplainCardRequest(BaseModel):
    front: str
    back: str


class ExplainCardResponse(BaseModel):
    explanation: str


class GenerateFromTextRequest(BaseModel):
    text: str
    is_url: bool = False
    count: int = 10
    difficulty: str = "medium"


class GenerateFromKBRequest(BaseModel):
    kb_name: str
    topic: str
    count: int = 10
    difficulty: str = "medium"


class SaveQuestionsRequest(BaseModel):
    deck_name: str
    questions: list[dict]
    source: str = "chat"
    topic: str = ""


class SaveQuestionsResponse(BaseModel):
    success: bool
    deck_id: Optional[int] = None
    flashcard_count: int
    flashcards: list[dict] = []
    message: str


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _cache_key(text: str, count: int, difficulty: str) -> str:
    payload = f"{count}:{difficulty}:{text}"
    return hashlib.md5(payload.encode("utf-8")).hexdigest()


def _get_cached(key: str) -> list | None:
    entry = _flashcard_cache.get(key)
    if not entry:
        return None
    if time.time() - entry["ts"] > _CACHE_TTL:
        del _flashcard_cache[key]
        return None
    return entry["flashcards"]


def _set_cache(key: str, flashcards: list) -> None:
    _flashcard_cache[key] = {"flashcards": flashcards, "ts": time.time()}


def _extract_text(file_bytes: bytes, filename: str) -> str:
    if filename.lower().endswith((".txt", ".md", ".markdown")):
        try:
            return file_bytes.decode("utf-8")
        except UnicodeDecodeError:
            return file_bytes.decode("latin-1", errors="replace")
    parser = PDFParser()
    return parser.parse(file_bytes)


def _flashcard_prompt(text: str, count: int, difficulty: str) -> str:
    return (
        f"Generate exactly {count} flashcards from the following text at {difficulty} difficulty level.\n"
        f"Each flashcard must have a 'front' (question) and 'back' (answer).\n"
        f"Return ONLY valid JSON in this format: {{\"flashcards\": [{{\"front\": \"...\", \"back\": \"...\"}}, ...]}}\n"
        f"Do not include any other text or explanation.\n\nText:\n{text}"
    )


def _sse(data: dict) -> str:
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@router.post("/generate-flashcards", response_model=GenerateFlashcardsResponse)
@limiter.limit("15/hour")
async def generate_flashcards(
    request: Request,
    file: UploadFile = File(...),
    count: int = Form(10),
    difficulty: str = Form("medium"),
):
    """Generate flashcards from a PDF / TXT / MD file."""
    try:
        file_bytes = await file.read()
        filename = (file.filename or "").lower()

        if filename.endswith((".txt", ".md", ".markdown")):
            try:
                text = file_bytes.decode("utf-8")
            except UnicodeDecodeError:
                text = file_bytes.decode("latin-1", errors="replace")
        else:
            parser = PDFParser()
            text = parser.parse(file_bytes)

        if not text or not text.strip():
            raise HTTPException(status_code=400, detail="Failed to extract text from file")

        text = text[:MAX_TEXT_LENGTH]

        key = _cache_key(text, count, difficulty)
        cached = _get_cached(key)
        if cached:
            return GenerateFlashcardsResponse(flashcards=cached[:count])

        llm_client = get_llm_client()
        response = await llm_client.complete(_flashcard_prompt(text, count, difficulty))

        try:
            flashcards = json.loads(response).get("flashcards", [])[:count]
            _set_cache(key, flashcards)

            # Detect language
            lang_code = detect_language(text)
            lang_name = get_language_name(lang_code)

            return GenerateFlashcardsResponse(
                flashcards=flashcards,
                language=lang_code,
                language_name=lang_name
            )
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except HTTPException:
        raise
    except Exception as e:
        import logging
        logging.getLogger("flashcard_gen").error(f"generate_flashcards error: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Error generating flashcards: {str(e)}")


@router.post("/generate-flashcards/stream")
@limiter.limit("15/hour")
async def generate_flashcards_stream(
    request: Request,
    file: UploadFile = File(...),
    count: int = Form(10),
    difficulty: str = Form("medium"),
):
    """Generate flashcards with SSE streaming — status updates + LLM chunks + final result."""
    file_bytes = await file.read()
    filename = (file.filename or "").lower()

    async def event_stream() -> AsyncGenerator[str, None]:
        try:
            yield _sse({"type": "status", "message": "Đang đọc file..."})
            text = _extract_text(file_bytes, filename)

            if not text or not text.strip():
                yield _sse({"type": "error", "message": "Không thể đọc nội dung file"})
                return

            text = text[:MAX_TEXT_LENGTH]

            key = _cache_key(text, count, difficulty)
            cached = _get_cached(key)
            if cached:
                yield _sse({"type": "status", "message": "Lấy từ cache..."})
                yield _sse({"type": "result", "flashcards": cached[:count], "cached": True})
                return

            yield _sse({"type": "status", "message": "Đang tạo flashcards..."})

            full_response = ""
            async for chunk in llm_factory.stream(prompt=_flashcard_prompt(text, count, difficulty)):
                full_response += chunk
                yield _sse({"type": "chunk", "content": chunk})

            try:
                flashcards = json.loads(full_response).get("flashcards", [])[:count]
                _set_cache(key, flashcards)

                # Detect language
                lang_code = detect_language(text)
                lang_name = get_language_name(lang_code)

                yield _sse({
                    "type": "result",
                    "flashcards": flashcards,
                    "language": lang_code,
                    "language_name": lang_name,
                    "cached": False
                })
            except json.JSONDecodeError:
                yield _sse({"type": "error", "message": "Không thể parse kết quả từ LLM"})

        except Exception as e:
            import logging
            logging.getLogger("flashcard_gen").error(f"stream error: {e}\n{traceback.format_exc()}")
            yield _sse({"type": "error", "message": str(e)})

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/generate-flashcards-from-kb", response_model=GenerateFlashcardsResponse)
@limiter.limit("15/hour")
async def generate_flashcards_from_kb(request: Request, body: GenerateFromKBRequest):
    """Generate flashcards from a knowledge base via RAG search."""
    try:
        from backend.services.rag.service import RAGService

        rag = RAGService()
        rag_result = await rag.search(query=body.topic, kb_name=body.kb_name)
        text = (rag_result.get("content") or rag_result.get("answer") or "").strip()

        if not text:
            raise HTTPException(
                status_code=404,
                detail=f"Không tìm thấy nội dung liên quan đến '{body.topic}' trong KB '{body.kb_name}'",
            )

        text = text[:MAX_TEXT_LENGTH]

        key = _cache_key(text, body.count, body.difficulty)
        cached = _get_cached(key)

        # Detect language once
        lang_code = detect_language(text)
        lang_name = get_language_name(lang_code)

        if cached:
            return GenerateFlashcardsResponse(
                flashcards=cached[:body.count],
                language=lang_code,
                language_name=lang_name
            )

        llm_client = get_llm_client()
        response = await llm_client.complete(_flashcard_prompt(text, body.count, body.difficulty))

        try:
            flashcards = json.loads(response).get("flashcards", [])[:body.count]
            _set_cache(key, flashcards)
            return GenerateFlashcardsResponse(
                flashcards=flashcards,
                language=lang_code,
                language_name=lang_name
            )
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except HTTPException:
        raise
    except Exception as e:
        import logging
        logging.getLogger("flashcard_gen").error(
            f"generate_flashcards_from_kb error: {e}\n{traceback.format_exc()}"
        )
        raise HTTPException(status_code=500, detail=f"Error generating flashcards from KB: {str(e)}")


@router.post("/explain-card", response_model=ExplainCardResponse)
async def explain_card(body: ExplainCardRequest):
    """Explain a flashcard."""
    try:
        llm_client = get_llm_client()
        prompt = (
            f"Explain this flashcard in detail. Help the student understand why this is important.\n\n"
            f"Front (Question): {body.front}\n"
            f"Back (Answer): {body.back}\n\n"
            f"Provide a clear, educational explanation."
        )
        explanation = await llm_client.complete(prompt)
        return ExplainCardResponse(explanation=explanation)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error explaining card: {str(e)}")


@router.post("/generate-from-text", response_model=GenerateFlashcardsResponse)
async def generate_from_text(body: GenerateFromTextRequest):
    """Generate flashcards from plain text or a URL."""
    try:
        text = body.text

        if body.is_url:
            async with httpx.AsyncClient() as client:
                response = await client.get(text, timeout=10)
                response.raise_for_status()
                text = response.text

        text = text[:MAX_TEXT_LENGTH]

        key = _cache_key(text, body.count, body.difficulty)
        cached = _get_cached(key)
        if cached:
            return GenerateFlashcardsResponse(flashcards=cached[:body.count])

        llm_client = get_llm_client()
        response = await llm_client.complete(_flashcard_prompt(text, body.count, body.difficulty))

        try:
            flashcards = json.loads(response).get("flashcards", [])[:body.count]
            _set_cache(key, flashcards)
            return GenerateFlashcardsResponse(flashcards=flashcards)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except httpx.RequestError as e:
        import logging
        logging.getLogger("flashcard_gen").error(f"URL fetch error: {e}")
        raise HTTPException(
            status_code=400,
            detail=f"Failed to fetch URL: {str(e)}. Please check if the URL is valid and accessible.",
        )
    except Exception as e:
        import logging
        logging.getLogger("flashcard_gen").error(
            f"generate_from_text error: {e}\n{traceback.format_exc()}"
        )
        raise HTTPException(status_code=500, detail=f"Error generating flashcards: {str(e)}")


@router.post("/save-questions-from-chat", response_model=SaveQuestionsResponse)
async def save_questions_from_chat(body: SaveQuestionsRequest):
    """Save generated questions as flashcards for later use in the app."""
    try:
        if not body.questions:
            raise HTTPException(status_code=400, detail="No questions provided")
        if not body.deck_name:
            raise HTTPException(status_code=400, detail="Deck name is required")

        flashcards = []
        for q in body.questions:
            if not isinstance(q, dict):
                continue

            front = q.get("question") or q.get("front") or ""
            back = q.get("answer") or q.get("correct_answer") or q.get("back") or ""
            question_type = q.get("question_type") or q.get("type") or "flashcard"
            explanation = q.get("explanation") or ""

            if question_type not in ("flashcard", "essay", "multiple_choice"):
                question_type = "essay" if not q.get("options") else "multiple_choice"

            if front and back:
                flashcards.append({
                    "front": str(front).strip(),
                    "back": str(back).strip(),
                    "questionType": question_type,
                    "explanation": str(explanation).strip(),
                    "source": body.source,
                    "topic": body.topic,
                    "createdAt": int(time.time() * 1000),
                })

        if not flashcards:
            raise HTTPException(status_code=400, detail="No valid questions found")

        return SaveQuestionsResponse(
            success=True,
            flashcard_count=len(flashcards),
            flashcards=flashcards,
            message=f"Đã chuẩn bị {len(flashcards)} câu hỏi từ {body.deck_name}",
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error saving questions: {str(e)}")
