from collections.abc import AsyncGenerator

from pydantic import BaseModel, Field

from backend.logging import get_logger

logger = get_logger(__name__)


class TutorResponse(BaseModel):
    content: str
    raw_response: dict[str, object] = Field(default_factory=dict)
    usage: dict[str, int] = Field(
        default_factory=lambda: {
            "prompt_tokens": 0,
            "completion_tokens": 0,
            "total_tokens": 0,
        }
    )
    provider: str = ""
    model: str = ""
    finish_reason: str | None = None
    cost_estimate: float = 0.0
    
class TutorStreamChunk(BaseModel):
    delta: str
    content: str = ""
    provider: str = ""
    model: str = ""
    is_complete: bool = False
    usage: dict[str, int] | None = None


AsyncStreamGenerator = AsyncGenerator[TutorStreamChunk, None]
LLMResponse = TutorResponse
StreamChunk = TutorStreamChunk

__all__ = [
    "AsyncStreamGenerator",
    "LLMResponse",
    "StreamChunk",
    "TutorResponse",
    "TutorStreamChunk",
]
