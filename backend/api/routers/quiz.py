"""Quiz generation from Knowledge Base."""

import json
import traceback
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from backend.services.llm import get_llm_client
from backend.services.rag.service import RAGService
from backend.api.rate_limiter import limiter

router = APIRouter()

MAX_TEXT_LENGTH = 120_000


class KbQuizRequest(BaseModel):
    kb_name: str
    topic: str
    count: int = 5
    difficulty: str = "medium"          # easy | medium | hard
    question_type: str = "mixed"        # multiple_choice | essay | mixed


class KbQuizQuestion(BaseModel):
    question: str
    correct_answer: str
    explanation: str = ""
    question_type: str                  # multiple_choice | essay
    options: list[str] = []            # only for multiple_choice
    correct_index: int = -1            # only for multiple_choice


class KbQuizResponse(BaseModel):
    kb_name: str
    topic: str
    questions: list[KbQuizQuestion]


def _build_prompt(text: str, count: int, difficulty: str, question_type: str) -> str:
    if question_type == "multiple_choice":
        type_instruction = (
            "All questions must be multiple-choice with exactly 4 options. "
            "Set question_type to 'multiple_choice', provide options as a list of 4 strings, "
            "and correct_index as the 0-based index of the correct option."
        )
    elif question_type == "essay":
        type_instruction = (
            "All questions must be open-ended/essay. "
            "Set question_type to 'essay', leave options as [] and correct_index as -1."
        )
    else:  # mixed
        type_instruction = (
            "Mix multiple-choice and essay questions (roughly half each). "
            "For multiple-choice: set question_type='multiple_choice', provide 4 options, set correct_index. "
            "For essay: set question_type='essay', options=[], correct_index=-1."
        )

    return (
        f"Generate exactly {count} quiz questions at {difficulty} difficulty from the text below.\n"
        f"{type_instruction}\n"
        f"Return ONLY valid JSON:\n"
        f'{{"questions": [{{"question": "...", "correct_answer": "...", "explanation": "...", '
        f'"question_type": "...", "options": [], "correct_index": -1}}]}}\n\n'
        f"Text:\n{text}"
    )


@router.post("/generate-from-kb", response_model=KbQuizResponse)
@limiter.limit("20/hour")
async def generate_quiz_from_kb(request: Request, body: KbQuizRequest):
    """Generate quiz questions from a knowledge base using RAG search."""
    try:
        rag = RAGService()
        rag_result = await rag.search(query=body.topic, kb_name=body.kb_name)
        text = (rag_result.get("content") or rag_result.get("answer") or "").strip()

        if not text:
            raise HTTPException(
                status_code=404,
                detail=f"Không tìm thấy nội dung về '{body.topic}' trong KB '{body.kb_name}'",
            )

        text = text[:MAX_TEXT_LENGTH]

        llm_client = get_llm_client()
        response = await llm_client.complete(
            _build_prompt(text, body.count, body.difficulty, body.question_type)
        )

        try:
            raw_questions = json.loads(response).get("questions", [])
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="LLM trả về định dạng không hợp lệ")

        questions: list[KbQuizQuestion] = []
        for q in raw_questions:
            if not isinstance(q, dict):
                continue
            qt = q.get("question_type", "essay")
            options = q.get("options", [])
            correct_index = q.get("correct_index", -1)

            # Sanitize multiple_choice
            if qt == "multiple_choice" and len(options) != 4:
                qt = "essay"
                options = []
                correct_index = -1

            questions.append(
                KbQuizQuestion(
                    question=str(q.get("question", "")).strip(),
                    correct_answer=str(q.get("correct_answer", "")).strip(),
                    explanation=str(q.get("explanation", "")).strip(),
                    question_type=qt,
                    options=[str(o) for o in options],
                    correct_index=int(correct_index),
                )
            )

        if not questions:
            raise HTTPException(status_code=500, detail="Không tạo được câu hỏi nào")

        return KbQuizResponse(kb_name=body.kb_name, topic=body.topic, questions=questions)

    except HTTPException:
        raise
    except Exception as e:
        import logging
        logging.getLogger("quiz").error(f"generate_quiz_from_kb error: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Lỗi tạo quiz: {str(e)}")
