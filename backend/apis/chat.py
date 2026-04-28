import uuid
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from google.genai import types
from core.ai import client
from core.storage import save_chat_message, get_chat_history

router = APIRouter(prefix="/api/v1", tags=["Chat AI"])

# --- Models ---
class ChatMessage(BaseModel):
    role: str  # "user" hoặc "model"
    text: str

class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None  # Nếu None, Backend sẽ tự tạo mới

class ChatResponse(BaseModel):
    answer: str
    session_id: str
    history: List[ChatMessage]

# --- Endpoints ---
@router.post("/chat", response_model=ChatResponse)
async def chat_interaction(request: ChatRequest):
    try:
        # 1. Xác định hoặc tạo session_id
        session_id = request.session_id or str(uuid.uuid4())
        
        # 2. Lấy lịch sử từ Database
        db_history = get_chat_history(session_id)
        
        # 3. Chuyển đổi lịch sử sang định dạng Gemini SDK
        gemini_history = []
        for msg in db_history:
            gemini_history.append(
                types.Content(
                    role=msg["role"],
                    parts=[types.Part(text=msg["text"])]
                )
            )

        # 4. Khởi tạo session chat với SDK
        chat_session = client.chats.create(
            model="gemini-3.1-flash-lite-preview",
            config=types.GenerateContentConfig(
                system_instruction="Bạn là một trợ lý học tập thân thiện và thông minh. Hãy giúp người dùng giải đáp thắc mắc về bài học."
            ),
            history=gemini_history
        )

        # 5. Lưu tin nhắn của User vào DB
        save_chat_message(session_id, "user", request.message)

        # 6. Gửi tin nhắn mới đến AI
        response = chat_session.send_message(request.message)
        
        # 7. Lưu câu trả lời của AI vào DB
        save_chat_message(session_id, "model", response.text.strip())
        
        # 8. Lấy lại lịch sử đầy đủ để trả về Client
        full_history = get_chat_history(session_id)
        chat_messages = [ChatMessage(role=m["role"], text=m["text"]) for m in full_history]

        return ChatResponse(
            answer=response.text.strip(),
            session_id=session_id,
            history=chat_messages
        )

    except Exception as e:
        print(f"Chat Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
