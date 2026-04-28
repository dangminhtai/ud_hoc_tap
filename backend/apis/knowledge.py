import os
import json
import uuid
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from core.ai import client
from core.db import collection, BASE_DIR

router = APIRouter(prefix="/api/v1", tags=["Knowledge Base"])

KNOWLEDGE_FILE = os.path.join(BASE_DIR, "datas", "knowledges.json")

# --- Models ---
class KbItemDto(BaseModel):
    name: str
    is_default: bool = False
    status: str = "ready"
    file_count: int = 0
    chunk_count: int = 0
    created_at: Optional[str] = None

class KbQuizRequest(BaseModel):
    kb_name: str
    topic: str
    count: int = 5
    difficulty: str = "medium"
    question_type: str = "mixed"

class KbQuizQuestion(BaseModel):
    question: str
    correct_answer: str
    explanation: str = ""
    question_type: str
    options: List[str] = []
    correct_index: int = -1

class KbQuizResponse(BaseModel):
    kb_name: str
    topic: str
    questions: List[KbQuizQuestion]

# --- Helpers ---
def get_relevant_context(query: str):
    """Tìm kiếm và trả về ngữ cảnh cùng nguồn tài liệu liên quan từ ChromaDB."""
    try:
        if collection.count() == 0:
            return "", []
            
        query_res = client.models.embed_content(
            model="gemini-embedding-001",
            contents=query
        )
        query_embedding = query_res.embeddings[0].values
        
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=3
        )
        
        relevant_docs = results["documents"][0]
        sources = [meta["title"] for meta in results["metadatas"][0]]
        
        return "\n\n".join(relevant_docs), list(set(sources))
    except Exception as e:
        print(f"Retrieval Error: {str(e)}")
        return "", []

def init_knowledge_base():
    """Nạp dữ liệu từ knowledges.json vào ChromaDB nếu bộ sưu tập trống."""
    if collection.count() == 0 and os.path.exists(KNOWLEDGE_FILE):
        print("Initializing Knowledge Base...")
        with open(KNOWLEDGE_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
            
        ids, documents, metadatas, embeddings = [], [], [], []
        
        for item in data:
            doc_text = f"Title: {item['title']}\nContent: {item['content']}"
            res = client.models.embed_content(model="gemini-embedding-001", contents=doc_text)
            ids.append(item["id"])
            documents.append(doc_text)
            metadatas.append({"title": item["title"]})
            embeddings.append(res.embeddings[0].values)
            
        collection.add(ids=ids, documents=documents, metadatas=metadatas, embeddings=embeddings)
        print(f"Successfully indexed {len(data)} items.")

init_knowledge_base()

# --- Endpoints ---

@router.get("/knowledge/list", response_model=List[KbItemDto])
async def list_kbs():
    """Liệt kê các Knowledge Base (Hiện tại fix cứng default KB)."""
    count = collection.count()
    return [
        KbItemDto(
            name="Hệ thống tri thức mặc định",
            is_default=True,
            status="ready",
            file_count=1,
            chunk_count=count,
            created_at="2024-04-28"
        )
    ]

@router.post("/knowledge/create", response_model=KbItemDto)
async def create_kb(name: str = Form(...), files: List[UploadFile] = File(...)):
    """Mock tạo KB mới (Trong thực tế sẽ xử lý file và lưu vào ChromaDB)."""
    # Vì demo nên chúng ta chỉ ghi nhận là đã nhận file
    return KbItemDto(
        name=name,
        is_default=False,
        status="processing",
        file_count=len(files)
    )

@router.post("/quiz/generate-from-kb", response_model=KbQuizResponse)
async def generate_quiz_from_kb(request: KbQuizRequest):
    """Tạo bộ câu hỏi trắc nghiệm từ một chủ đề trong Knowledge Base."""
    try:
        # 1. Lấy ngữ cảnh liên quan đến Topic
        context, _ = get_relevant_context(request.topic)
        
        # 2. Dùng Gemini tạo JSON trắc nghiệm
        prompt = f"""
        Dựa trên ngữ cảnh sau đây, hãy tạo {request.count} câu hỏi trắc nghiệm về chủ đề '{request.topic}'.
        Độ khó: {request.difficulty}.
        
        YÊU CẦU ĐỊNH DẠNG JSON:
        Trả về một mảng các đối tượng có cấu trúc:
        {{
            "question": "Nội dung câu hỏi",
            "correct_answer": "Nội dung đáp án đúng",
            "explanation": "Giải thích tại sao đúng",
            "question_type": "multiple_choice",
            "options": ["Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D"],
            "correct_index": 0
        }}
        
        --- NGỮ CẢNH ---
        {context if context else 'Sử dụng kiến thức chung nếu không có ngữ cảnh.'}
        """
        
        response = client.models.generate_content(
            model="gemini-3.1-flash-lite-preview",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
            )
        )
        
        questions_data = json.loads(response.text)
        # Handle if it's a list or wrapped in an object
        if isinstance(questions_data, dict):
             # Try to find a list inside
             for key in questions_data:
                 if isinstance(questions_data[key], list):
                     questions_data = questions_data[key]
                     break
        
        return KbQuizResponse(
            kb_name=request.kb_name,
            topic=request.topic,
            questions=questions_data[:request.count]
        )
        
    except Exception as e:
        print(f"Quiz Gen Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
