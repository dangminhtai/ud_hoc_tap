import json
import httpx
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

        # Truncate text to reasonable size (~8000 chars for context)
        text = text[:8000]

        # Call LLM
        llm_client = get_llm_client()
        prompt = f"""Generate exactly {count} flashcards from the following text at {difficulty} difficulty level.
Each flashcard must have a 'front' (question) and 'back' (answer).
Return ONLY valid JSON in this format: {{"flashcards": [{{"front": "...", "back": "..."}}, ...]}}
Do not include any other text or explanation.

Text:
{text}"""

        response = await llm_client.generate_text(prompt)

        # Parse JSON response
        try:
            result = json.loads(response)
            flashcards = result.get("flashcards", [])
            if len(flashcards) > count:
                flashcards = flashcards[:count]
            return GenerateFlashcardsResponse(flashcards=flashcards)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail="Failed to parse LLM response as JSON")

    except Exception as e:
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

        explanation = await llm_client.generate_text(prompt)
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

        # Truncate to reasonable size
        text = text[:8000]

        # Call LLM
        llm_client = get_llm_client()
        prompt = f"""Generate exactly {body.count} flashcards from the following text at {body.difficulty} difficulty level.
Each flashcard must have a 'front' (question) and 'back' (answer).
Return ONLY valid JSON in this format: {{"flashcards": [{{"front": "...", "back": "..."}}, ...]}}
Do not include any other text or explanation.

Text:
{text}"""

        response = await llm_client.generate_text(prompt)

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
        raise HTTPException(status_code=400, detail=f"Failed to fetch URL: {str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error generating flashcards: {str(e)}")
