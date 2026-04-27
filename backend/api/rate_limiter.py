"""Rate limiting configuration for API endpoints."""

from slowapi import Limiter
from slowapi.util import get_remote_address

limiter = Limiter(
    key_func=get_remote_address,
    default_limits=["200 per day", "50 per hour"],
    storage_uri="memory://",
)

# Rate limit configurations by endpoint
LIMITS = {
    # AI features (more restrictive - expensive operations)
    "ai_chat": "30 per hour",
    "ai_question": "20 per hour",
    "generate_quiz": "20 per hour",
    "generate_flashcards": "15 per hour",

    # Knowledge base (moderate)
    "knowledge_list": "100 per hour",
    "knowledge_upload": "10 per hour",

    # Regular endpoints (permissive)
    "flashcard_crud": "200 per hour",
    "review_log": "500 per hour",
    "deck_crud": "100 per hour",
}
