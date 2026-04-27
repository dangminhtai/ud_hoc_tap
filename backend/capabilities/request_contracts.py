"""Public request contracts and config validators for built-in capabilities."""

from __future__ import annotations

from typing import Any, Callable, Literal

from pydantic import BaseModel, ConfigDict, Field, ValidationError

_RUNTIME_ONLY_KEYS = {"_persist_user_message", "followup_question_context"}


class ChatRequestConfig(BaseModel):
    """Chat request config class."""
    model_config = ConfigDict(extra="forbid")


class DeepSolveRequestConfig(BaseModel):
    """Deep solve request config class."""
    model_config = ConfigDict(extra="forbid")

    detailed_answer: bool = True


class DeepQuestionRequestConfig(BaseModel):
    """Deep question request config class."""
    model_config = ConfigDict(extra="forbid")

    mode: Literal["custom", "mimic"] = "custom"
    topic: str = ""
    num_questions: int = Field(default=1, ge=1, le=50)
    difficulty: str = ""
    question_type: str = ""
    preference: str = ""
    paper_path: str = ""
    max_questions: int = Field(default=10, ge=1, le=100)


# Placeholder classes for removed agents
class DeepResearchRequestConfig(BaseModel):
    """Deep research request config (removed)."""
    model_config = ConfigDict(extra="forbid")


class MathAnimatorRequestConfig(BaseModel):
    """Math animator request config (removed)."""
    model_config = ConfigDict(extra="forbid")


def validate_research_request_config(raw_config: dict[str, Any] | None) -> DeepResearchRequestConfig:
    """Validate deep research request config (removed)."""
    return _validate_model(DeepResearchRequestConfig, raw_config, label="deep research")


def validate_math_animator_request_config(raw_config: dict[str, Any] | None) -> MathAnimatorRequestConfig:
    """Validate math animator request config (removed)."""
    return _validate_model(MathAnimatorRequestConfig, raw_config, label="math animator")


def _clean_public_config(raw_config: dict[str, Any] | None) -> dict[str, Any]:
    """Clean public config."""
    if raw_config is None:
        return {}
    if not isinstance(raw_config, dict):
        raise ValueError("Capability config must be an object.")
    cleaned = dict(raw_config)
    for key in _RUNTIME_ONLY_KEYS:
        cleaned.pop(key, None)
    return cleaned


def _validate_model(
    model_type: type[BaseModel],
    raw_config: dict[str, Any] | None,
    *,
    label: str,
) -> BaseModel:
    """Validate model."""
    cleaned = _clean_public_config(raw_config)
    try:
        return model_type.model_validate(cleaned)
    except ValidationError as exc:
        details = "; ".join(
            f"{'.'.join(str(part) for part in error['loc'])}: {error['msg']}"
            for error in exc.errors()
        )
        raise ValueError(f"Invalid {label} config: {details}") from exc


def validate_chat_request_config(raw_config: dict[str, Any] | None) -> ChatRequestConfig:
    """Validate chat request config."""
    return _validate_model(ChatRequestConfig, raw_config, label="chat")


def validate_deep_solve_request_config(
    raw_config: dict[str, Any] | None,
) -> DeepSolveRequestConfig:
    """Validate deep solve request config."""
    return _validate_model(DeepSolveRequestConfig, raw_config, label="deep solve")


def validate_deep_question_request_config(
    raw_config: dict[str, Any] | None,
) -> DeepQuestionRequestConfig:
    """Validate deep question request config."""
    return _validate_model(DeepQuestionRequestConfig, raw_config, label="deep question")


def build_request_schema(model_type: type[BaseModel]) -> dict[str, Any]:
    """Build and return request schema."""
    return model_type.model_json_schema(mode="validation")


CAPABILITY_CONFIG_VALIDATORS: dict[str, Callable[[dict[str, Any] | None], Any]] = {
    "chat": validate_chat_request_config,
    "deep_solve": validate_deep_solve_request_config,
    "deep_question": validate_deep_question_request_config,
}

CAPABILITY_REQUEST_SCHEMAS: dict[str, dict[str, Any]] = {
    "chat": build_request_schema(ChatRequestConfig),
    "deep_solve": build_request_schema(DeepSolveRequestConfig),
    "deep_question": build_request_schema(DeepQuestionRequestConfig),
}


def validate_capability_config(capability: str, raw_config: dict[str, Any] | None) -> dict[str, Any]:
    """Validate capability config."""
    validator = CAPABILITY_CONFIG_VALIDATORS.get(capability)
    if validator is None:
        return _clean_public_config(raw_config)
    model = validator(raw_config)
    if isinstance(model, BaseModel):
        return model.model_dump(exclude_none=True)
    return _clean_public_config(raw_config)


def get_capability_request_schema(capability: str) -> dict[str, Any]:
    """Return the capability request schema."""
    return dict(CAPABILITY_REQUEST_SCHEMAS.get(capability, {}))


__all__ = [
    "CAPABILITY_CONFIG_VALIDATORS",
    "CAPABILITY_REQUEST_SCHEMAS",
    "ChatRequestConfig",
    "DeepQuestionRequestConfig",
    "DeepSolveRequestConfig",
    "DeepResearchRequestConfig",
    "MathAnimatorRequestConfig",
    "build_request_schema",
    "get_capability_request_schema",
    "validate_capability_config",
    "validate_chat_request_config",
    "validate_deep_question_request_config",
    "validate_deep_solve_request_config",
    "validate_research_request_config",
    "validate_math_animator_request_config",
]
