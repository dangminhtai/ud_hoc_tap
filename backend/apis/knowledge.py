import os
import json
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from core.ai import client
from core.db import collection, BASE_DIR

router = APIRouter(prefix="/api/v1", tags=["Knowledge Base"])

KNOWLEDGE_FILE = os.path.join(BASE_DIR, "datas", "knowledges.json")

# --- Models ---
class KnowledgeRequest(BaseModel):
    query: str

class KnowledgeResponse(BaseModel):
    answer: str
    sources: list[str]

# --- Helpers ---
def init_knowledge_base():
    """Nạp dữ liệu từ knowledges.json vào ChromaDB nếu bộ sưu tập trống."""
    if collection.count() == 0 and os.path.exists(KNOWLEDGE_FILE):
        print("Initializing Knowledge Base...")
        with open(KNOWLEDGE_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
            
        ids = []
        documents = []
        metadatas = []
        embeddings = []
        
        for item in data:
            doc_text = f"Title: {item['title']}\nContent: {item['content']}"
            
            res = client.models.embed_content(
                model="gemini-embedding-001",
                contents=doc_text
            )
            
            ids.append(item["id"])
            documents.append(doc_text)
            metadatas.append({"title": item["title"]})
            embeddings.append(res.embeddings[0].values)
            
        collection.add(
            ids=ids,
            documents=documents,
            metadatas=metadatas,
            embeddings=embeddings
        )
        print(f"Successfully indexed {len(data)} knowledge items.")

# Khởi tạo ngay khi module được import
init_knowledge_base()

# --- Endpoints ---
@router.post("/ask-knowledge", response_model=KnowledgeResponse)
async def ask_knowledge(request: KnowledgeRequest):
    try:
        # 1. Chuyển câu hỏi của user thành Vector
        query_res = client.models.embed_content(
            model="gemini-embedding-001",
            contents=request.query
        )
        query_embedding = query_res.embeddings[0].values
        
        # 2. Tìm kiếm Top 3 đoạn liên quan nhất
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=3
        )
        
        relevant_docs = results["documents"][0]
        sources = [meta["title"] for meta in results["metadatas"][0]]
        
        if not relevant_docs:
            return KnowledgeResponse(
                answer="Xin lỗi, mình không tìm thấy thông tin này trong bộ tri thức.",
                sources=[]
            )
            
        # 3. Tổng hợp câu trả lời dùng Gemini
        context = "\n\n".join(relevant_docs)
        prompt = f"""
        Bạn là một trợ lý học tập thông minh. Hãy dựa vào NGỮ CẢNH được cung cấp bên dưới để trả lời CÂU HỎI của người dùng.
        Nếu thông tin không có trong ngữ cảnh, hãy nói rằng bạn không biết, đừng tự bịa ra thông tin.
        
        YÊU CẦU: Trả lời bằng tiếng Việt, thân thiện và súc tích.
        
        --- NGỮ CẢNH ---
        {context}
        
        --- CÂU HỎI ---
        {request.query}
        """
        
        gen_res = client.models.generate_content(
            model="gemini-3.1-flash-lite-preview",
            contents=prompt
        )
        
        return KnowledgeResponse(
            answer=gen_res.text.strip(),
            sources=list(set(sources))
        )
        
    except Exception as e:
        print(f"RAG Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
