import json
import time
import uuid
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any

from backend.services.path_service import get_path_service


class BaseSessionManager(ABC):
    MAX_SESSIONS = 100  
    
    def __init__(self, module_name: str):
        self.module_name = module_name
        self.path_service = get_path_service()
        self.sessions_file = self.path_service.get_session_file(module_name)
        self._ensure_file()

    @abstractmethod
    def _get_session_id_prefix(self) -> str:
        pass
    
    @abstractmethod
    def _get_default_title(self) -> str:
        pass
    
    @abstractmethod
    def _create_session_data(self, **kwargs) -> dict[str, Any]:
        pass
    
    @abstractmethod
    def _get_session_summary(self, session: dict[str, Any]) -> dict[str, Any]:
        pass
    
    def _ensure_file(self) -> None:
        self.sessions_file.parent.mkdir(parents=True, exist_ok=True)
        
        if not self.sessions_file.exists():
            initial_data = {
                "version": "1.0",
                "sessions": [],
            }
            self._save_data(initial_data)
    
    def _load_data(self) -> dict[str, Any]:
        try:
            with open(self.sessions_file, encoding="utf-8") as f:
                return json.load(f)
        except (json.JSONDecodeError, FileNotFoundError):
            return {"version": "1.0", "sessions": []}
    
    def _save_data(self, data: dict[str, Any]) -> None:
        with open(self.sessions_file, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
    
    def _get_sessions(self) -> list[dict[str, Any]]:
        data = self._load_data()
        return data.get("sessions", [])
    
    def _save_sessions(self, sessions: list[dict[str, Any]]) -> None:
        """Save sessions list."""
        data = self._load_data()
        data["sessions"] = sessions
        self._save_data(data)
    
    def create_session(
        self,
        title: str | None = None,
        **kwargs,
    ) -> dict[str, Any]:
        prefix = self._get_session_id_prefix()
        session_id = f"{prefix}{int(time.time() * 1000)}_{uuid.uuid4().hex[:8]}"
        now = time.time()
        
        if title is None:
            title = self._get_default_title()
        
        session = {
            "session_id": session_id,
            "title": title[:100] if title else self._get_default_title(),  # Limit title length
            "messages": [],
            "created_at": now,
            "updated_at": now,
        }
        
        module_data = self._create_session_data(**kwargs)
        session.update(module_data)
        
        sessions = self._get_sessions()
        sessions.insert(0, session)  # Add to front (newest first)
        
        # Limit total sessions
        if len(sessions) > self.MAX_SESSIONS:
            sessions = sessions[:self.MAX_SESSIONS]
        
        self._save_sessions(sessions)
        
        return session
    
    def get_session(self, session_id: str) -> dict[str, Any] | None:
        sessions = self._get_sessions()
        for session in sessions:
            if session.get("session_id") == session_id:
                return session
        return None
    
    def update_session(
        self,
        session_id: str,
        messages: list[dict[str, Any]] | None = None,
        title: str | None = None,
        **kwargs,
    ) -> dict[str, Any] | None:
        sessions = self._get_sessions()
        
        for i, session in enumerate(sessions):
            if session.get("session_id") == session_id:
                if messages is not None:
                    session["messages"] = messages
                if title is not None:
                    session["title"] = title[:100]
                
                # Update module-specific fields
                for key, value in kwargs.items():
                    if value is not None:
                        session[key] = value
                
                session["updated_at"] = time.time()
                
                # Move to front (most recently updated)
                sessions.pop(i)
                sessions.insert(0, session)
                
                self._save_sessions(sessions)
                return session
        
        return None
    
    def add_message(
        self,
        session_id: str,
        role: str,
        content: str,
        **metadata,
    ) -> dict[str, Any] | None:
        session = self.get_session(session_id)
        if not session:
            return None
        
        message = {
            "role": role,
            "content": content,
            "timestamp": time.time(),
        }
        
        for key, value in metadata.items():
            if value is not None:
                message[key] = value
        
        messages = session.get("messages", [])
        messages.append(message)
        
        title = None
        if session.get("title") == self._get_default_title() and role == "user":
            title = content[:50] + ("..." if len(content) > 50 else "")
        
        return self.update_session(session_id, messages=messages, title=title)
    
    def list_sessions(
        self,
        limit: int = 20,
        include_messages: bool = False,
    ) -> list[dict[str, Any]]:
        sessions = self._get_sessions()[:limit]
        
        if not include_messages:
            # Return summary only (without full messages)
            return [self._get_session_summary(s) for s in sessions]
        
        return sessions
    
    def delete_session(self, session_id: str) -> bool:
        sessions = self._get_sessions()
        original_count = len(sessions)
        
        sessions = [s for s in sessions if s.get("session_id") != session_id]
        
        if len(sessions) < original_count:
            self._save_sessions(sessions)
            return True
        
        return False
    
    def clear_all_sessions(self) -> int:
        sessions = self._get_sessions()
        count = len(sessions)
        self._save_sessions([])
        return count

    def get_session_count(self) -> int:
        return len(self._get_sessions())
    
    def session_exists(self, session_id: str) -> bool:
        return self.get_session(session_id) is not None


__all__ = ["BaseSessionManager"]
