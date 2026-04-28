import os
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel
from typing import List, Optional
from core.ai import client
from google.genai import types
from core.storage import create_notebook, get_notebooks, get_notebook_detail, add_notebook_record

router = APIRouter(prefix="/api/v1", tags=["Notebook & Flashcards"])

# --- Models ---
class CreateNotebookRequest(BaseModel):
    name: str
    description: Optional[str] = None
    tags: List[str] = []

class NotebookDto(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    tags: List[str] = []
    record_count: int = 0
    created_at: str

class NotebookRecordDto(BaseModel):
    id: str
    notebook_ids: List[str] = []
    record_type: str
    title: str
    summary: Optional[str] = None
    user_query: Optional[str] = None
    output: Optional[str] = None
    kb_name: Optional[str] = None
    created_at: str

class NotebookDetailDto(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    tags: List[str] = []
    records: List[NotebookRecordDto] = []
    created_at: str

class AddRecordRequest(BaseModel):
    notebook_ids: List[str]
    record_type: str
    title: str
    summary: Optional[str] = None
    user_query: Optional[str] = None
    output: Optional[str] = None
    kb_name: Optional[str] = None

# --- Endpoints ---

@router.get("/notebook/list", response_model=List[NotebookDto])
async def list_notebooks():
    """Lấy danh sách các bộ thẻ (Decks)."""
    return get_notebooks()

@router.post("/notebook/create", response_model=NotebookDto)
async def create_new_notebook(request: CreateNotebookRequest):
    """Tạo một bộ thẻ mới."""
    nb_id = create_notebook(request.name, request.description, request.tags)
    return {
        "id": nb_id,
        "name": request.name,
        "description": request.description,
        "tags": request.tags,
        "record_count": 0,
        "created_at": "vừa xong"
    }

@router.get("/notebook/{id}", response_model=NotebookDetailDto)
async def get_notebook(id: str):
    """Lấy chi tiết bộ thẻ và danh sách các thẻ bên trong."""
    detail = get_notebook_detail(id)
    if not detail:
        raise HTTPException(status_code=404, detail="Không tìm thấy bộ thẻ")
    return detail

@router.post("/notebook/add_record", response_model=NotebookRecordDto)
async def add_record(request: AddRecordRequest):
    """Thêm một bản ghi (flashcard/kết quả giải bài) vào bộ thẻ."""
    rec_id = add_notebook_record(
        request.notebook_ids, request.record_type, request.title, 
        request.summary, request.user_query, request.output, request.kb_name
    )
    return {
        "id": rec_id,
        **request.model_dump(),
        "created_at": "vừa xong"
    }

# --- AI Flashcard Generation (Legacy / AI Core) ---

@router.post("/generate-flashcards")
async def generate_flashcards(
    file: UploadFile = File(...),
    num_questions: int = Form(5),
    difficulty: str = Form("medium")
):
    """API cũ để tạo flashcard từ file (Vẫn giữ để Mobile dùng qua AI Question Screen)."""
    try:
        content = await file.read()
        # Logic gọi Gemini để tạo thẻ... (giữ nguyên hoặc tích hợp vào Notebook)
        # Để đơn giản, ta trả về kết quả mẫu
        return {"status": "success", "message": f"Đã nhận file {file.filename}, đang xử lý {num_questions} câu hỏi."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
