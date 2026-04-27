from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

@dataclass
class Attachment:
    type: str  # "image" | "file" | "pdf"
    url: str = ""
    base64: str = ""
    filename: str = ""
    mime_type: str = ""
    
@dataclass
class UnifiedContext:   
    session_id: str = ""
    user_message: str = ""
    conversation_history: list[dict[str, Any]] = field(default_factory=list)
    enabled_tools: list[str] | None = None
    active_capability: str | None = None
    knowledge_bases: list[str] = field(default_factory=list)
    attachments: list[Attachment] = field(default_factory=list)
    config_overrides: dict[str, Any] = field(default_factory=dict)
    language: str = "en"
    notebook_context: str = ""
    history_context: str = ""
    memory_context: str = ""
    metadata: dict[str, Any] = field(default_factory=dict)
