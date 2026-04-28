import os
import json
import uuid
import asyncio
from fastapi import APIRouter, HTTPException, UploadFile, File, Form, WebSocket
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from google.genai import types
from core.ai import client
from core.db import collection, BASE_DIR
from core.storage import save_kb_file, get_kb_files

router = APIRouter(prefix="/api/v1", tags=["Knowledge Base"])

KNOWLEDGE_FILE = os.path.join(BASE_DIR, "datas", "knowledges.json")
UPLOAD_DIR = os.path.join(BASE_DIR, "datas", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

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
    """Liệt kê các Knowledge Base."""
    count = collection.count()
    default_files = get_kb_files("default")
    
    kbs = [
        KbItemDto(
            name="default",
            is_default=True,
            status="ready",
            file_count=len(default_files) + 1,
            chunk_count=count,
            created_at="2024-04-28"
        )
    ]
    
    # Lấy các KB khác từ SQLite (nếu có files)
    conn = collection._client # ChromaDB internal client access is messy, let's use SQLite
    import sqlite3
    from core.storage import DB_PATH
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT kb_name FROM kb_files WHERE kb_name != 'default'")
    rows = cursor.fetchall()
    for row in rows:
        kb_name = row[0]
        files = get_kb_files(kb_name)
        kbs.append(KbItemDto(
            name=kb_name,
            is_default=False,
            status="ready",
            file_count=len(files),
            created_at="2024-04-28"
        ))
    conn.close()
    return kbs

@router.get("/knowledge/default", response_model=KbItemDto)
async def get_default_kb():
    """Lấy tri thức mặc định."""
    count = collection.count()
    default_files = get_kb_files("default")
    return KbItemDto(
        name="default",
        is_default=True,
        status="ready",
        file_count=len(default_files) + 1,
        chunk_count=count,
        created_at="2024-04-28"
    )

@router.post("/knowledge/create", response_model=KbItemDto)
async def create_new_kb(name: str = Form(...), files: List[UploadFile] = File(...)):
    """Tạo KB mới và upload file lên Gemini File API."""
    for file in files:
        file_path = os.path.join(UPLOAD_DIR, file.filename)
        with open(file_path, "wb") as f:
            f.write(await file.read())
            
        gemini_file = client.files.upload(
            file=file_path, 
            config={'display_name': file.filename, 'mime_type': file.content_type}
        )
        save_kb_file(name, file.filename, gemini_file.uri, file.content_type)
        
    return KbItemDto(
        name=name,
        is_default=False,
        status="ready",
        file_count=len(files)
    )

@router.post("/knowledge/{name}/upload")
async def upload_to_existing_kb(name: str, files: List[UploadFile] = File(...)):
    """Upload thêm file vào một Knowledge Base đã có."""
    for file in files:
        file_path = os.path.join(UPLOAD_DIR, file.filename)
        with open(file_path, "wb") as f:
            f.write(await file.read())
            
        gemini_file = client.files.upload(
            file=file_path, 
            config={'display_name': file.filename, 'mime_type': file.content_type}
        )
        save_kb_file(name, file.filename, gemini_file.uri, file.content_type)
        
    return {"status": "success", "message": f"Đã tải lên {len(files)} file vào {name}"}

@router.websocket("/knowledge/{name}/progress/ws")
async def knowledge_progress_ws(websocket: WebSocket, name: str):
    """WebSocket theo dõi tiến độ xử lý Knowledge Base."""
    await websocket.accept()
    try:
        stages = [
            {"status": "processing", "progress": 20, "message": "Đang khởi tạo..."},
            {"status": "processing", "progress": 50, "message": "Đang trích xuất dữ liệu..."},
            {"status": "processing", "progress": 80, "message": "Đang tối ưu hóa bộ nhớ..."},
            {"status": "ready", "progress": 100, "message": "Hoàn thành!"}
        ]
        for stage in stages:
            await websocket.send_json(stage)
            await asyncio.sleep(0.5)
    except Exception as e:
        print(f"KB Progress WS Error: {str(e)}")
    finally:
        try:
            # Chỉ đóng nếu chưa đóng
            await websocket.close()
        except:
            pass

@router.put("/knowledge/default/{name}")
async def set_default_kb(name: str):
    """Đặt một Knowledge Base làm mặc định."""
    # Trong thực tế sẽ lưu vào settings/DB. Ở đây mock success.
    return {"status": "success", "message": f"Đã đặt '{name}' làm mặc định"}

@router.delete("/knowledge/{name}")
async def delete_kb(name: str):
    """Xóa một Knowledge Base."""
    # Xóa files trong SQLite
    import sqlite3
    from core.storage import DB_PATH
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM kb_files WHERE kb_name = ?", (name,))
    conn.commit()
    conn.close()
    
    # Xóa trong ChromaDB nếu name khớp (ChromaDB id là kb_name trong demo của chúng ta)
    # Tuy nhiên ids trong ChromaDB của chúng ta hiện tại là kb-1, kb-2... 
    # Nếu là KB tự tạo thì chúng ta chưa lưu vào ChromaDB (chỉ dùng Gemini File API).
    
    return {"status": "success", "message": f"Đã xóa kho tri thức '{name}'"}

@router.post("/quiz/generate-from-kb", response_model=KbQuizResponse)
async def generate_quiz_from_kb(request: KbQuizRequest):
    """Tạo bộ câu hỏi trắc nghiệm từ một chủ đề trong Knowledge Base."""
    try:
        context, _ = get_relevant_context(request.topic)
        prompt = f"""
        Dựa trên ngữ cảnh sau đây, hãy tạo {request.count} câu hỏi trắc nghiệm về chủ đề '{request.topic}'.
        Độ khó: {request.difficulty}.
        
        YÊU CẦU ĐỊNH DẠNG JSON:
        Trả về một mảng các đối tượng:
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
            config=types.GenerateContentConfig(response_mime_type="application/json")
        )
        questions_data = json.loads(response.text)
        if isinstance(questions_data, dict):
             for key in questions_data:
                 if isinstance(questions_data[key], list):
                     questions_data = questions_data[key]; break
        return KbQuizResponse(kb_name=request.kb_name, topic=request.topic, questions=questions_data[:request.count])
    except Exception as e:
        print(f"Quiz Gen Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
