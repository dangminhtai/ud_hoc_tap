import json
import uuid
import asyncio
from datetime import datetime
from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect, Query
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from google.genai import types
from core.ai import client
from core.storage import (
    save_chat_message, 
    get_chat_history, 
    list_chat_sessions, 
    delete_chat_session
)
from apis.knowledge import get_relevant_context

router = APIRouter(prefix="/api/v1", tags=["Chat AI"])

# --- REST Models ---
class ChatMessageDto(BaseModel):
    role: str
    content: str
    timestamp: Optional[float] = None
    sources: Optional[Dict[str, Any]] = None

class FullChatSessionDto(BaseModel):
    session_id: str
    title: Optional[str] = None
    messages: List[ChatMessageDto] = []
    settings: Optional[Dict[str, Any]] = None
    created_at: Optional[float] = None
    updated_at: Optional[float] = None
    message_count: Optional[int] = None
    last_message: Optional[str] = None

class ChatSessionListResponse(BaseModel):
    sessions: List[FullChatSessionDto]

# --- WebSocket Chat Endpoint ---
@router.websocket("/chat")
async def websocket_chat(websocket: WebSocket):
    await websocket.accept()
    session_id = None
    
    try:
        data = await websocket.receive_text()
        payload = json.loads(data)
        
        user_message = payload.get("message")
        session_id = payload.get("session_id") or str(uuid.uuid4())
        enable_rag = payload.get("enable_rag", False)
        enable_web_search = payload.get("enable_web_search", False)
        # Chấp nhận cả chuỗi đơn hoặc danh sách các kho tri thức
        kb_selection = payload.get("kb_names") or payload.get("kb_name") or []
        kb_names = [kb_selection] if isinstance(kb_selection, str) else kb_selection
        
        await websocket.send_json({
            "type": "session",
            "session_id": session_id
        })
        
        context = ""
        sources = []
        gemini_files = []
        
        if enable_rag:
            kb_display = ", ".join(kb_names) if kb_names else "tri thức mặc định"
            await websocket.send_json({
                "type": "status",
                "stage": "searching",
                "message": f"Đang tìm kiếm trong '{kb_display}'..."
            })
            
            # 1. Tìm trong ChromaDB (Dữ liệu mặc định - luôn tìm nếu bật RAG)
            context, sources = get_relevant_context(user_message)
            
            # 2. Tìm tất cả các file Gemini từ các kho tri thức đã chọn
            from core.storage import get_kb_files
            for name in kb_names:
                kb_files = get_kb_files(name)
                for f in kb_files:
                    gemini_files.append(types.Part(file_data=types.FileData(file_uri=f["file_uri"], mime_type=f["file_type"])))

        db_history = get_chat_history(session_id)
        gemini_history = []
        for msg in db_history:
            gemini_history.append(
                types.Content(role=msg["role"], parts=[types.Part(text=msg["text"])])
            )
            
        tools = []
        if enable_web_search:
            tools.append(types.Tool(google_search=types.GoogleSearch()))

        system_instruction = "Bạn là một trợ lý học tập thông minh. Trả lời bằng tiếng Việt, thân thiện."
        if context:
            system_instruction += f"\nSử dụng NGỮ CẢNH từ ChromaDB sau để trả lời:\n{context}"
        if gemini_files:
            system_instruction += "\nNgoài ra, tôi đã đính kèm các tài liệu PDF/TXT từ Knowledge Base. Hãy đọc chúng để trả lời chính xác nhất."

        model_name = "gemini-2.5-flash-lite"
        try:
            print(f"DEBUG: Đang khởi tạo model {model_name} với tools: {tools}")
            chat_session = client.chats.create(
                model=model_name,
                config=types.GenerateContentConfig(
                    system_instruction=system_instruction,
                    tools=tools
                ),
                history=gemini_history
            )
        except Exception as e:
            print(f"DEBUG: Lỗi khi tạo phiên chat: {str(e)}")
            await websocket.send_json({
                "type": "error",
                "message": f"Lỗi khởi tạo Chat: {str(e)}"
            })
            return

        save_chat_message(session_id, "user", user_message)

        full_response_text = ""
        try:
            await websocket.send_json({
                "type": "status",
                "stage": "generating",
                "message": "AI đang suy nghĩ..."
            })
            
            # Gửi tin nhắn kèm theo references tới files nếu có
            message_content = [user_message] + gemini_files
            full_response_text = ""
            grounding_metadata = None
            print(f"DEBUG: Đang gửi tin nhắn tới Gemini... (RAG: {enable_rag}, Search: {enable_web_search})")
            
            response_stream = chat_session.send_message_stream(message_content)
            
            for chunk in response_stream:
                print(f"DEBUG: Nhận chunk: {chunk}")
                if chunk.text:
                    full_response_text += chunk.text
                    await websocket.send_json({
                        "type": "stream",
                        "content": chunk.text
                    })
                # Lưu lại metadata từ chunk có chứa nó
                if chunk.candidates and chunk.candidates[0].grounding_metadata:
                    grounding_metadata = chunk.candidates[0].grounding_metadata
                    print("DEBUG: Đã nhận Grounding Metadata từ Google Search")
            
            print(f"DEBUG: Kết thúc stream. Tổng độ dài văn bản: {len(full_response_text)}")
            
            await websocket.send_json({
                "type": "result",
                "content": full_response_text
            })
            
            # Trích xuất nguồn tin cậy
            web_sources = []
            if grounding_metadata and grounding_metadata.grounding_chunks:
                for chunk in grounding_metadata.grounding_chunks:
                    if chunk.web:
                        web_sources.append({
                            "title": chunk.web.title or "Nguồn tin",
                            "url": chunk.web.uri
                        })

            if sources or web_sources:
                await websocket.send_json({
                    "type": "sources",
                    "rag": [{"title": s} for s in sources],
                    "web": web_sources
                })

            save_chat_message(session_id, "model", full_response_text)

        except Exception as e:
            await websocket.send_json({
                "type": "error",
                "message": f"Lỗi Gemini: {str(e)}"
            })

    except WebSocketDisconnect:
        print(f"WS Disconnected: {session_id}")
    except Exception as e:
        print(f"WS Error: {str(e)}")
        try: await websocket.send_json({"type": "error", "message": str(e)})
        except: pass
    finally:
        try: await websocket.close()
        except: pass

# --- Session Management Endpoints ---

@router.get("/chat/sessions", response_model=ChatSessionListResponse)
async def list_sessions(limit: int = Query(30)):
    """Lấy danh sách các phiên chat cho Mobile Sidebar."""
    sessions = list_chat_sessions(limit)
    full_sessions = []
    for s in sessions:
        full_sessions.append(FullChatSessionDto(
            session_id=s["session_id"],
            title=s["title"],
            message_count=s["message_count"],
            updated_at=datetime.strptime(s["updated_at"], "%Y-%m-%d %H:%M:%S").timestamp() if isinstance(s["updated_at"], str) else None,
            created_at=datetime.strptime(s["created_at"], "%Y-%m-%d %H:%M:%S").timestamp() if isinstance(s["created_at"], str) else None
        ))
    return ChatSessionListResponse(sessions=full_sessions)

@router.get("/chat/sessions/{session_id}", response_model=FullChatSessionDto)
async def get_session_detail(session_id: str):
    """Lấy chi tiết lịch sử tin nhắn của một phiên chat."""
    history = get_chat_history(session_id)
    if not history and session_id != "new":
         # Trả về rỗng thay vì lỗi để Mobile dễ xử lý
         return FullChatSessionDto(session_id=session_id, messages=[])
         
    messages = []
    for msg in history:
        messages.append(ChatMessageDto(
            role="user" if msg["role"] == "user" else "assistant",
            content=msg["text"]
        ))
        
    return FullChatSessionDto(
        session_id=session_id,
        messages=messages,
        message_count=len(messages)
    )

@router.delete("/chat/sessions/{session_id}")
async def delete_session(session_id: str):
    """Xóa một phiên chat."""
    success = delete_chat_session(session_id)
    return {"status": "success", "session_id": session_id}
