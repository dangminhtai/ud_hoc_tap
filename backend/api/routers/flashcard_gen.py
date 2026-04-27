import json
import httpx
import time
import traceback
from typing import Optional
from fastapi import APIRouter, UploadFile, File, HTTPException, Form
from pydantic import BaseModel
from backend.services.llm import get_llm_client
from backend.services.rag.components.parsers.pdf import PDFParser

router = APIRouter()


class GenerateFlashcardsResponse(BaseModel):
    flashcards: list[dict[str, str]]


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


@router.post("/generate-flashcards", response_model=GenerateFlashcardsResponse)
async def generate_flashcards(
    file: UploadFile = File(...),
    count: int = Form(10),
    difficulty: str = Form("medium")
):
    """Generate flashcards from a PDF file."""
    try:
        # Read PDF file
        pdf_content = await file.read()

        # Parse PDF text
        parser = PDFParser()
        text = parser.parse(pdf_content)

        if not text or not text.strip():
            raise HTTPException(status_code=400, detail="Failed to extract text from PDF")

        # Increase context window to ~200,000 chars for modern large-context LLMs
        # This solves the issue of generating too few questions for large PDFs
        text = text[:]

        # Call LLM
        llm_client = get_llm_client()
        prompt = f"""Generate exactly {count} flashcards from the following text at {difficulty} difficulty level.
Each flashcard must have a 'front' (question) and 'back' (answer).
Return ONLY valid JSON in this format: {{"flashcards": [{{"front": "...", "back": "..."}}, ...]}}
Do not include any other text or explanation.

Text:
{text}"""

        response = await llm_client.complete(prompt)

        # Parse JSON response
        try:
            result = json.loads(response)
            flashcards = result.get("flashcards", [])
            if len(flashcards) > count:
                flashcards = flashcards[:count]
            return GenerateFlashcardsResponse(flashcards=flashcards)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except HTTPException:
        raise
    except Exception as e:
        tb = traceback.format_exc()
        # Log full traceback so it appears in server logs
        import logging
        logging.getLogger("flashcard_gen").error(f"generate_flashcards error: {e}\n{tb}")
        raise HTTPException(status_code=500, detail=f"Error generating flashcards: {str(e)}")


@router.post("/explain-card", response_model=ExplainCardResponse)
async def explain_card(body: ExplainCardRequest):
    """Explain a flashcard."""
    try:
        llm_client = get_llm_client()
        prompt = f"""Explain this flashcard in detail. Help the student understand why this is important.

Front (Question): {body.front}
Back (Answer): {body.back}

Provide a clear, educational explanation."""

        explanation = await llm_client.complete(prompt)
        return ExplainCardResponse(explanation=explanation)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error explaining card: {str(e)}")


@router.post("/generate-from-text", response_model=GenerateFlashcardsResponse)
async def generate_from_text(body: GenerateFromTextRequest):
    """Generate flashcards from text or URL."""
    try:
        text = body.text

        # Fetch from URL if needed
        if body.is_url:
            async with httpx.AsyncClient() as client:
                response = await client.get(text, timeout=10)
                response.raise_for_status()
                text = response.text

        # Increase context window to ~200,000 chars
        text = text[:200000]

        # Call LLM
        llm_client = get_llm_client()
        prompt = f"""Generate exactly {body.count} flashcards from the following text at {body.difficulty} difficulty level.
Each flashcard must have a 'front' (question) and 'back' (answer).
Return ONLY valid JSON in this format: {{"flashcards": [{{"front": "...", "back": "..."}}, ...]}}
Do not include any other text or explanation.

Text:
{text}"""

        response = await llm_client.complete(prompt)

        # Parse JSON response
        try:
            result = json.loads(response)
            flashcards = result.get("flashcards", [])
            if len(flashcards) > body.count:
                flashcards = flashcards[:body.count]
            return GenerateFlashcardsResponse(flashcards=flashcards)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except httpx.RequestError as e:
        import logging
        logging.getLogger("flashcard_gen").error(f"URL fetch error: {e}")
        raise HTTPException(status_code=400, detail=f"Failed to fetch URL: {str(e)}. Please check if the URL is valid and accessible.")
    except Exception as e:
        tb = traceback.format_exc()
        import logging
        logging.getLogger("flashcard_gen").error(f"generate_from_text error: {e}\n{tb}")
        raise HTTPException(status_code=500, detail=f"Error generating flashcards: {str(e)}")


class SaveQuestionsRequest(BaseModel):
    """Request to save generated questions as flashcards."""
    deck_name: str
    questions: list[dict]  # [{"question": "...", "answer": "...", "question_type": "essay", "explanation": "..."}]
    source: str = "chat"  # "chat", "exam_mimic", "pdf"
    topic: str = ""


class SaveQuestionsResponse(BaseModel):
    """Response after saving questions."""
    success: bool
    deck_id: Optional[int] = None
    flashcard_count: int
    flashcards: list[dict] = []
    message: str


@router.post("/save-questions-from-chat", response_model=SaveQuestionsResponse)
async def save_questions_from_chat(body: SaveQuestionsRequest):
    """
    Save generated questions as flashcards for later use in the app.

    Supports essay and multiple-choice question types with explanations.
    """
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

            # Normalize question_type
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
