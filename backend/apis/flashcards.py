import re
import urllib.request
from fastapi import APIRouter, File, Form, UploadFile, HTTPException
from pydantic import BaseModel
from google.genai import types
from core.ai import client

router = APIRouter(prefix="/api/v1", tags=["Flashcards"])

# --- Models ---
class Flashcard(BaseModel):
    front: str
    back: str

class GenerateResponse(BaseModel):
    flashcards: list[Flashcard]

class ExplainRequest(BaseModel):
    front: str
    back: str

class ExplainResponse(BaseModel):
    explanation: str

class TextRequest(BaseModel):
    text: str
    is_url: bool = False
    count: int = 10
    difficulty: str = "medium"

# --- Constants ---
DIFFICULTY_INSTRUCTIONS = {
    "easy": (
        "Tạo câu hỏi ĐƠN GIẢN ở mức độ DỄ:\n"
        "- Câu hỏi nhận biết, ghi nhớ cơ bản (What is? Define? List...)\n"
        "- Đáp án ngắn gọn, rõ ràng, dễ nhớ\n"
        "- Không đòi hỏi suy luận sâu\n"
        "- Phù hợp để học thuộc lần đầu"
    ),
    "medium": (
        "Tạo câu hỏi ở mức độ TRUNG BÌNH:\n"
        "- Kết hợp nhận biết và hiểu biết (What? How? Why in simple terms?)\n"
        "- Câu hỏi bao quát các khái niệm chính\n"
        "- Đáp án giải thích đủ ý nhưng súc tích"
    ),
    "hard": (
        "Tạo câu hỏi ở mức độ KHÓ, đòi hỏi tư duy sâu:\n"
        "- Phân tích, so sánh, đánh giá (Why? Compare X and Y? What would happen if? How does X affect Y?)\n"
        "- Câu hỏi ứng dụng vào tình huống thực tế\n"
        "- Câu hỏi kết hợp nhiều khái niệm với nhau\n"
        "- Đáp án cần giải thích cơ chế hoặc lý do"
    ),
}

# --- Helpers ---
async def generate_flashcards_with_gemini(
    file_bytes: bytes,
    mime_type: str,
    count: int = 10,
    difficulty: str = "medium"
) -> list[Flashcard]:
    difficulty_guide = DIFFICULTY_INSTRUCTIONS.get(difficulty, DIFFICULTY_INSTRUCTIONS["medium"])
    prompt = f"""
    Bạn là một gia sư chuyên gia tạo flashcard học tập từ tài liệu được cung cấp.

    {difficulty_guide}

    QUY TẮC BẮT BUỘC:
    - 'front' PHẢI là CÂU HỎI (không được là định nghĩa hay nội dung thô)
    - 'back' PHẢI là câu trả lời rõ ràng, súc tích cho câu hỏi đó
    - Tạo ĐÚNG {count} flashcard (không nhiều hơn, không ít hơn)
    - Câu hỏi phải bằng CÙNG NGÔN NGỮ với tài liệu
    - Bao phủ các khái niệm, quy trình, công thức và sự kiện quan trọng nhất
    """

    try:
        content_parts = [prompt]
        if mime_type == "text/plain":
            content_parts.append(file_bytes.decode("utf-8"))
        else:
            content_parts.append(
                types.Part.from_bytes(data=file_bytes, mime_type=mime_type)
            )

        response = client.models.generate_content(
            model="gemini-3.1-flash-lite-preview",
            contents=content_parts,
            config={
                "response_mime_type": "application/json",
                "response_schema": list[Flashcard],
            },
        )
        return response.parsed
    except Exception as e:
        raise Exception(f"AI Generation failed: {str(e)}")

# --- Endpoints ---
@router.post("/generate-flashcards", response_model=GenerateResponse)
async def generate_flashcards(
    file: UploadFile = File(...),
    count: int = Form(default=10),
    difficulty: str = Form(default="medium"),
):
    ALLOWED_TYPES = {
        "application/pdf": "application/pdf",
        "text/plain": "text/plain",
    }
    mime_type = file.content_type
    if mime_type not in ALLOWED_TYPES:
        if file.filename.endswith(".pdf"):
            mime_type = "application/pdf"
        elif file.filename.endswith(".txt"):
            mime_type = "text/plain"
        else:
            raise HTTPException(status_code=400, detail="Only PDF and TXT files are supported")

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="File is empty")

    count = max(1, min(count, 50))

    try:
        flashcards = await generate_flashcards_with_gemini(content, mime_type, count, difficulty)
        return GenerateResponse(flashcards=flashcards)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/explain-card", response_model=ExplainResponse)
async def explain_card(request: ExplainRequest):
    prompt = f"""
    Bạn là một gia sư chuyên gia. Hãy giải thích chi tiết thẻ học sau bằng TIẾNG VIỆT:

    Mặt trước: {request.front}
    Mặt sau: {request.back}

    Cung cấp:
    1. Giải thích rõ ràng, dễ hiểu về khái niệm này
    2. Ví dụ thực tế hoặc ứng dụng (nếu có)
    3. Các điểm liên quan quan trọng cần nhớ

    YÊU CẦU: Trả lời HOÀN TOÀN bằng tiếng Việt. Ngắn gọn nhưng đầy đủ (tối đa 200 từ). Chỉ trả văn bản thuần túy, không dùng markdown.
    """
    try:
        response = client.models.generate_content(
            model="gemini-3.1-flash-lite-preview",
            contents=[prompt],
        )
        return ExplainResponse(explanation=response.text.strip())
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/generate-from-text", response_model=GenerateResponse)
async def generate_from_text(request: TextRequest):
    content = request.text
    if request.is_url:
        try:
            req = urllib.request.Request(
                request.text, headers={"User-Agent": "Mozilla/5.0"}
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                raw = resp.read().decode("utf-8", errors="ignore")
            content = re.sub(r"<[^>]+>", " ", raw)
            content = re.sub(r"\s+", " ", content).strip()
            content = content[:12000]
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Cannot fetch URL: {str(e)}")

    if not content.strip():
        raise HTTPException(status_code=400, detail="Content is empty")

    count = max(1, min(request.count, 50))

    try:
        flashcards = await generate_flashcards_with_gemini(
            content.encode("utf-8"), "text/plain", count, request.difficulty
        )
        return GenerateResponse(flashcards=flashcards)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
