"""
Chat API Router
================

WebSocket endpoint for lightweight chat with session management.
REST endpoints for session operations.
"""

from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect

from backend.agents.chat import ChatAgent, SessionManager
from backend.loggers import get_logger
from backend.services.config import PROJECT_ROOT, load_config_with_main
from backend.services.llm.config import get_llm_config
from backend.services.settings.interface_settings import get_ui_language

# Initialize logger
config = load_config_with_main("main.yaml", PROJECT_ROOT)
log_dir = config.get("paths", {}).get("user_log_dir") or config.get("logging", {}).get("log_dir")
logger = get_logger("ChatAPI", level="INFO", log_dir=log_dir)

router = APIRouter()

# Initialize session manager
session_manager = SessionManager()


# =============================================================================
# REST Endpoints for Session Management
# =============================================================================


@router.get("/chat/sessions")
async def list_sessions(limit: int = 20):
    """
    List recent chat sessions.

    Args:
        limit: Maximum number of sessions to return

    Returns:
        Dict with sessions list (matches ChatSessionListResponse on Android)
    """
    sessions = session_manager.list_sessions(limit=limit, include_messages=False)
    return {"sessions": sessions}


@router.get("/chat/sessions/{session_id}")
async def get_session(session_id: str):
    """
    Get a specific chat session with full message history.

    Args:
        session_id: Session identifier

    Returns:
        Complete session data including messages
    """
    session = session_manager.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    return session


@router.delete("/chat/sessions/{session_id}")
async def delete_session(session_id: str):
    """
    Delete a chat session.

    Args:
        session_id: Session identifier

    Returns:
        Success message
    """
    if session_manager.delete_session(session_id):
        return {"status": "deleted", "session_id": session_id}
    raise HTTPException(status_code=404, detail="Session not found")


# =============================================================================
# WebSocket Endpoint for Chat
# =============================================================================


@router.websocket("/chat")
async def websocket_chat(websocket: WebSocket):
    """
    WebSocket endpoint for chat with session and context management.

    Message format:
    {
        "message": str,              # User message
        "session_id": str | null,    # Session ID (null for new session)
        "history": [...] | null,     # Optional: explicit history override
        "kb_name": str,              # Knowledge base name (for RAG)
        "enable_rag": bool,          # Enable RAG retrieval
        "enable_web_search": bool    # Enable Web Search
    }

    Response format:
    - {"type": "session", "session_id": str}           # Session ID (new or existing)
    - {"type": "status", "stage": str, "message": str} # Status updates
    - {"type": "stream", "content": str}               # Streaming response chunks
    - {"type": "sources", "rag": list, "web": list}    # Source citations
    - {"type": "result", "content": str}               # Final complete response
    - {"type": "error", "message": str}                # Error message
    """
    await websocket.accept()

    try:
        while True:
            # Receive message
            data = await websocket.receive_json()
            # Use current UI language (fallback to config/main.yaml system.language)
            language = get_ui_language(default=config.get("system", {}).get("language", "en"))
            message = data.get("message", "").strip()
            session_id = data.get("session_id")
            explicit_history = data.get("history")  # Optional override
            kb_name = data.get("kb_name", "").strip()
            enable_rag = data.get("enable_rag", False)
            enable_web_search = data.get("enable_web_search", False)

            # If RAG enabled but no KB specified, use default KB
            if enable_rag and not kb_name:
                try:
                    from backend.knowledge.manager import KnowledgeBaseManager
                    manager = KnowledgeBaseManager()
                    kb_name = manager.get_default()
                    if kb_name:
                        logger.info(f"Using default KB: {kb_name}")
                except Exception as kb_err:
                    logger.warning(f"Could not get default KB: {kb_err}")

            if not message:
                await websocket.send_json({"type": "error", "message": "Message is required"})
                continue

            if enable_rag and not kb_name:
                await websocket.send_json({
                    "type": "error",
                    "message": "RAG enabled but no knowledge base available. Please create or select a knowledge base."
                })
                continue

            logger.info(
                f"Chat request: session={session_id}, "
                f"message={message[:50]}..., rag={enable_rag}, kb={kb_name}, web={enable_web_search}"
            )

            try:
                # Check for special actions (e.g., question generation, save)
                action = data.get("action")

                if action == "save_questions":
                    # Handle saving questions to flashcard
                    from fastapi import HTTPException as FastAPIException
                    from backend.api.routers.flashcard_gen import SaveQuestionsRequest

                    try:
                        req = SaveQuestionsRequest(
                            deck_name=data.get("deck_name", "Chat Questions"),
                            questions=data.get("questions", []),
                            source="chat",
                            topic=data.get("topic", ""),
                        )

                        logger.info(f"Saving {len(req.questions)} questions to flashcard")

                        await websocket.send_json(
                            {
                                "type": "status",
                                "stage": "saving",
                                "message": "Đang lưu questions vào flashcard...",
                            }
                        )

                        # Save questions
                        from backend.api.routers.flashcard_gen import save_questions_from_chat
                        result = await save_questions_from_chat(req)

                        await websocket.send_json(
                            {
                                "type": "result",
                                "content": f"✅ {result.message}",
                            }
                        )
                    except Exception as e:
                        logger.error(f"Error saving questions: {e}")
                        await websocket.send_json(
                            {
                                "type": "error",
                                "message": f"Lỗi khi lưu: {str(e)}",
                            }
                        )
                    continue

                if action == "generate_questions":
                    # Handle question generation
                    option = data.get("question_option", "")
                    details = data.get("question_details", {})

                    logger.info(f"Generating questions: option={option}")

                    try:
                        llm_config = get_llm_config()
                        api_key = llm_config.api_key
                        base_url = llm_config.base_url
                        api_version = getattr(llm_config, "api_version", None)
                    except Exception:
                        api_key = None
                        base_url = None
                        api_version = None

                    agent = ChatAgent(
                        language=get_ui_language(default=config.get("system", {}).get("language", "en")),
                        config=config,
                        api_key=api_key,
                        base_url=base_url,
                        api_version=api_version,
                    )

                    # Generate questions
                    await websocket.send_json(
                        {
                            "type": "status",
                            "stage": "generating",
                            "message": "Đang tạo questions...",
                        }
                    )

                    result = await agent.generate_questions(
                        option=option,
                        details=details,
                        kb_name=kb_name,
                    )

                    if result.get("success"):
                        await websocket.send_json(
                            {
                                "type": "questions",
                                "data": result,
                                "option": option,
                            }
                        )
                        await websocket.send_json(
                            {
                                "type": "result",
                                "content": f"Đã tạo {result.get('completed', 0)} questions!",
                            }
                        )
                    else:
                        await websocket.send_json(
                            {
                                "type": "error",
                                "message": result.get("error", "Failed to generate questions"),
                            }
                        )
                    continue

                # Get or create session
                if session_id:
                    session = session_manager.get_session(session_id)
                    if not session:
                        # Session not found, create new one
                        session = session_manager.create_session(
                            title=message[:50] + ("..." if len(message) > 50 else ""),
                            settings={
                                "kb_name": kb_name,
                                "enable_rag": enable_rag,
                                "enable_web_search": enable_web_search,
                            },
                        )
                        session_id = session["session_id"]
                else:
                    # Create new session
                    session = session_manager.create_session(
                        title=message[:50] + ("..." if len(message) > 50 else ""),
                        settings={
                            "kb_name": kb_name,
                            "enable_rag": enable_rag,
                            "enable_web_search": enable_web_search,
                        },
                    )
                    session_id = session["session_id"]

                # Send session ID to frontend with KB info
                await websocket.send_json(
                    {
                        "type": "session",
                        "session_id": session_id,
                        "kb_name": kb_name,
                        "rag_enabled": enable_rag,
                        "rag_status": "ready" if kb_name else "no_kb",
                    }
                )

                # Send chat history if session has messages
                session_messages = session.get("messages", [])
                if session_messages:
                    await websocket.send_json(
                        {
                            "type": "history",
                            "messages": [
                                {
                                    "role": msg.get("role", "user"),
                                    "content": msg.get("content", ""),
                                    "sources": msg.get("sources"),
                                    "timestamp": msg.get("timestamp"),
                                }
                                for msg in session_messages
                            ],
                            "count": len(session_messages),
                        }
                    )

                # Build history from session or explicit override
                if explicit_history is not None:
                    history = explicit_history
                else:
                    # Get history from session messages
                    history = [
                        {"role": msg["role"], "content": msg["content"]}
                        for msg in session.get("messages", [])
                    ]

                # Add user message to session
                session_manager.add_message(
                    session_id=session_id,
                    role="user",
                    content=message,
                )

                # Send user message to frontend
                await websocket.send_json(
                    {
                        "type": "message",
                        "role": "user",
                        "content": message,
                    }
                )

                # Initialize ChatAgent
                try:
                    llm_config = get_llm_config()
                    api_key = llm_config.api_key
                    base_url = llm_config.base_url
                    api_version = getattr(llm_config, "api_version", None)
                except Exception:
                    api_key = None
                    base_url = None
                    api_version = None

                agent = ChatAgent(
                    language=language,
                    config=config,
                    api_key=api_key,
                    base_url=base_url,
                    api_version=api_version,
                )

                # Send status updates
                if enable_rag and kb_name:
                    await websocket.send_json(
                        {
                            "type": "status",
                            "stage": "rag",
                            "message": f"Searching knowledge base: {kb_name}...",
                            "kb_name": kb_name,
                            "kb_active": True,
                        }
                    )

                if enable_web_search:
                    await websocket.send_json(
                        {
                            "type": "status",
                            "stage": "web",
                            "message": "Searching the web...",
                        }
                    )

                await websocket.send_json(
                    {
                        "type": "status",
                        "stage": "generating",
                        "message": "Generating response...",
                    }
                )

                # Process with streaming
                full_response = ""
                sources = {"rag": [], "web": []}

                stream_generator = await agent.process(
                    message=message,
                    history=history,
                    kb_name=kb_name,
                    enable_rag=enable_rag,
                    enable_web_search=enable_web_search,
                    stream=True,
                )

                async for chunk_data in stream_generator:
                    if chunk_data["type"] == "menu":
                        await websocket.send_json(chunk_data)
                        continue

                    if chunk_data["type"] == "chunk":
                        await websocket.send_json(
                            {
                                "type": "stream",
                                "content": chunk_data["content"],
                            }
                        )
                        full_response += chunk_data["content"]
                    elif chunk_data["type"] == "complete":
                        full_response = chunk_data["response"]
                        sources = chunk_data.get("sources", {"rag": [], "web": []})

                # Send sources if any
                logger.info(f"Chat sources - RAG: {len(sources.get('rag', []))}, Web: {len(sources.get('web', []))}")
                if sources.get("rag"):
                    for src in sources["rag"]:
                        logger.debug(f"Source: {src.get('title', src.get('kb_name', 'unknown'))}")

                if sources.get("rag") or sources.get("web"):
                    sources["kb_name"] = kb_name
                    sources["rag_enabled"] = enable_rag
                    await websocket.send_json({"type": "sources", **sources})
                else:
                    logger.warning(f"No sources found - enable_rag: {enable_rag}, kb_name: {kb_name}")

                # Send final result with metadata
                await websocket.send_json(
                    {
                        "type": "result",
                        "content": full_response,
                        "kb_name": kb_name,
                        "sources_count": len(sources.get("rag", [])) + len(sources.get("web", [])),
                        "has_sources": bool(sources.get("rag") or sources.get("web")),
                    }
                )

                # Save assistant message to session
                session_manager.add_message(
                    session_id=session_id,
                    role="assistant",
                    content=full_response,
                    sources=sources if (sources.get("rag") or sources.get("web")) else None,
                )

                logger.info(f"Chat completed: session={session_id}, {len(full_response)} chars")

            except Exception as e:
                logger.error(f"Chat processing error: {e}")
                await websocket.send_json({"type": "error", "message": str(e)})

    except WebSocketDisconnect:
        logger.debug("Client disconnected from chat")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        try:
            await websocket.send_json({"type": "error", "message": str(e)})
        except Exception:
            pass


@router.get("/sessions/{session_id}/info")
async def get_session_info(session_id: str):
    """Get session info including KB name and RAG status."""
    try:
        session = session_manager.get_session(session_id)
        if not session:
            return {"status": "error", "message": "Session not found"}

        settings = session.get("settings", {})
        kb_name = settings.get("kb_name", "")
        enable_rag = settings.get("enable_rag", False)

        kb_status = "no_kb"
        if kb_name and enable_rag:
            try:
                from backend.knowledge.manager import KnowledgeBaseManager
                manager = KnowledgeBaseManager()
                kb_info = manager.get_info(kb_name)
                kb_status = kb_info.get("status", "unknown")
            except Exception:
                kb_status = "unknown"

        return {
            "session_id": session_id,
            "kb_name": kb_name,
            "rag_enabled": enable_rag,
            "kb_status": kb_status,
            "message_count": len(session.get("messages", [])),
        }
    except Exception as e:
        logger.error(f"Error getting session info: {e}")
        return {"status": "error", "message": str(e)}


@router.get("/test-rag")
async def test_rag_endpoint(kb_name: str = "AI ML", query: str = "test"):
    """Test RAG endpoint - diagnose why sources aren't appearing."""
    try:
        from backend.agents.chat import ChatAgent

        llm_config = get_llm_config()
        agent = ChatAgent(
            language="en",
            config=config,
            api_key=llm_config.api_key,
            base_url=llm_config.base_url,
        )

        logger.info(f"Testing RAG with kb_name={kb_name}, query={query}")

        context, sources = await agent.retrieve_context(
            message=query,
            kb_name=kb_name,
            enable_rag=True,
            enable_web_search=False,
        )

        return {
            "status": "ok",
            "kb_name": kb_name,
            "query": query,
            "context_length": len(context),
            "rag_sources_count": len(sources.get("rag", [])),
            "sources": sources,
            "first_200_context": context[:200] if context else "No context",
        }
    except Exception as e:
        logger.error(f"RAG test failed: {e}", exc_info=True)
        return {
            "status": "error",
            "error": str(e),
            "kb_name": kb_name,
        }
