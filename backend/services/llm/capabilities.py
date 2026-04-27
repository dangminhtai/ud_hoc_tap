PROVIDER_CAPABILITIES: dict[str, dict[str, object]] = {
    "openai": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,  # System prompt goes in messages array
        "newer_models_use_max_completion_tokens": True,
    },
    "azure_openai": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,
        "newer_models_use_max_completion_tokens": True,
        "requires_api_version": True,
    },
    "anthropic": {
        "supports_response_format": False,  
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": False,  
        "has_thinking_tags": False,
    },
    "claude": {
        "supports_response_format": False,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": False,
        "has_thinking_tags": False,
    },
    "deepseek": {
        "supports_response_format": False, 
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": False,
        "system_in_messages": True,
        "has_thinking_tags": True,  
    },
    "openrouter": {
        "supports_response_format": True,  
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,  
        "system_in_messages": True,
    },
    "groq": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,
    },
    "together": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,
    },
    "together_ai": {  
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,
    },
    "mistral": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": True,
        "supports_vision": True,
        "system_in_messages": True,
    },
    "ollama": {
        "supports_response_format": True, 
        "supports_streaming": True,
        "supports_tools": False, 
        "supports_vision": False,  
        "system_in_messages": True,
    },
    "lm_studio": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": False,
        "supports_vision": False,
        "system_in_messages": True,
    },
    "vllm": {
        "supports_response_format": True,
        "supports_streaming": True,
        "supports_tools": False,
        "supports_vision": False,
        "system_in_messages": True,
    },
    "llama_cpp": {
        "supports_response_format": True, 
        "supports_streaming": True,
        "supports_tools": False,
        "supports_vision": False,
        "system_in_messages": True,
    },
}

DEFAULT_CAPABILITIES: dict[str, object] = {
    "supports_response_format": True,
    "supports_streaming": True,
    "supports_tools": False,
    "supports_vision": False,
    "system_in_messages": True,
    "has_thinking_tags": False,
    "forced_temperature": None, 
}

MODEL_OVERRIDES: dict[str, dict[str, object]] = {
    "deepseek": {
        "supports_response_format": False,
        "has_thinking_tags": True,
        "supports_vision": False,
    },
    "deepseek-reasoner": {
        "supports_response_format": False,
        "has_thinking_tags": True,
        "supports_vision": False,
    },
    "qwen": {
        "has_thinking_tags": True,
    },
    "qwq": {
        "has_thinking_tags": True,
    },
    "gpt-5": {
        "forced_temperature": 1.0,
    },
    "o1": {
        "forced_temperature": 1.0,
    },
    "o3": {
        "forced_temperature": 1.0,
    },
    "gpt-4o": {"supports_vision": True},
    "gpt-4-turbo": {"supports_vision": True},
    "gpt-4-vision": {"supports_vision": True},
    "claude-3": {"supports_vision": True},
    "claude-4": {"supports_vision": True},
    "gemini": {"supports_vision": True},
    "gemma": {"supports_vision": False},
    "llava": {"supports_vision": True},
    "bakllava": {"supports_vision": True},
    "moondream": {"supports_vision": True},
    "minicpm-v": {"supports_vision": True},
    "gpt-3.5": {"supports_vision": False},
}

def get_capability(
    binding: str,
    capability: str,
    model: str | None = None,
    default: object = None,
) -> object:
    binding_lower = (binding or "openai").lower()

    # 1. Check model-specific overrides first
    if model:
        model_lower = model.lower()
        # Sort by pattern length descending to match most specific first
        for pattern, overrides in sorted(MODEL_OVERRIDES.items(), key=lambda x: -len(x[0])):
            if model_lower.startswith(pattern):
                if capability in overrides:
                    return overrides[capability]

    # 2. Check provider capabilities
    provider_caps = PROVIDER_CAPABILITIES.get(binding_lower, {})
    if capability in provider_caps:
        return provider_caps[capability]

    # 3. Check default capabilities for unknown providers
    if capability in DEFAULT_CAPABILITIES:
        return DEFAULT_CAPABILITIES[capability]

    # 4. Return explicit default
    return default


def supports_response_format(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "supports_response_format", model, default=True)
    return bool(value)


def supports_streaming(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "supports_streaming", model, default=True)
    return bool(value)

def system_in_messages(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "system_in_messages", model, default=True)
    return bool(value)

def has_thinking_tags(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "has_thinking_tags", model, default=False)
    return bool(value)

def supports_tools(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "supports_tools", model, default=False)
    return bool(value)

def supports_vision(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "supports_vision", model, default=False)
    return bool(value)

def requires_api_version(binding: str, model: str | None = None) -> bool:
    value = get_capability(binding, "requires_api_version", model, default=False)
    return bool(value)

def get_effective_temperature(
    binding: str,
    model: str | None = None,
    requested_temp: float = 0.7,
) -> float:
    forced_temp = get_capability(binding, "forced_temperature", model)
    if isinstance(forced_temp, (int, float)):
        return float(forced_temp)
    return requested_temp

__all__ = [
    "PROVIDER_CAPABILITIES",
    "MODEL_OVERRIDES",
    "DEFAULT_CAPABILITIES",
    "get_capability",
    "supports_response_format",
    "supports_streaming",
    "system_in_messages",
    "has_thinking_tags",
    "supports_tools",
    "supports_vision",
    "requires_api_version",
    "get_effective_temperature",
]
