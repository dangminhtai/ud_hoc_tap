"""Error mapping for LLM providers."""

import logging
from typing import Any, Optional

from .exceptions import (
    LLMAPIError,
    LLMAuthenticationError,
    LLMRateLimitError,
    LLMTimeoutError,
)

logger = logging.getLogger(__name__)

def map_error(exc: Exception, provider: str = "openai") -> Exception:
    """
    Map provider-specific errors to unified LLM exceptions.
    
    If no mapping is found, returns the original exception.
    """
    # Basic mapping for common libraries
    exc_name = type(exc).__name__
    msg = str(exc)
    
    # OpenAI
    if "openai" in provider.lower() or "azure" in provider.lower():
        if exc_name in ("RateLimitError", "RateLimitError"):
            return LLMRateLimitError(msg, provider=provider)
        if exc_name in ("APITimeoutError", "TimeoutError"):
            return LLMTimeoutError(msg, provider=provider)
        if exc_name in ("AuthenticationError", "PermissionDeniedError"):
            return LLMAuthenticationError(msg, provider=provider)
        if exc_name == "BadRequestError":
            return LLMAPIError(msg, status_code=400, provider=provider)
            
    # Generic status code extraction if available
    status_code = getattr(exc, "status_code", getattr(exc, "status", None))
    if status_code:
        if status_code == 429:
            return LLMRateLimitError(msg, provider=provider)
        if status_code in (401, 403):
            return LLMAuthenticationError(msg, provider=provider)
        if status_code == 408:
            return LLMTimeoutError(msg, provider=provider)
            
    return exc
