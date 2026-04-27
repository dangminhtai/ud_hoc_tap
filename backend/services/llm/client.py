from collections.abc import Awaitable, Callable
from typing import cast

from backend.logging import get_logger

from .config import LLMConfig, get_llm_config
from .utils import sanitize_url

class LLMClient:
    def __init__(self, config: LLMConfig | None = None) -> None:
        self.config = config or get_llm_config()
        self.logger = get_logger("LLMClient")
        self._setup_openai_env_vars()
        
    def _setup_openai_env_vars(self) -> None:
        import os

        binding = getattr(self.config, "binding", "openai")

        if binding in ("openai", "azure_openai", "gemini"):
            if self.config.api_key:
                os.environ["OPENAI_API_KEY"] = self.config.api_key
                self.logger.debug("Set OPENAI_API_KEY env var")

            if self.config.base_url:
                from .utils import sanitize_url as _sanitize
                clean_url = _sanitize(self.config.base_url)
                os.environ["OPENAI_BASE_URL"] = clean_url
                self.logger.debug(f"Set OPENAI_BASE_URL env var to {clean_url}")

    async def complete(
        self,
        prompt: str,
        system_prompt: str | None = None,
        history: list[dict[str, str]] | None = None,
        **kwargs: object,
    ) -> str:
        from . import factory

        factory_complete = cast(Callable[..., Awaitable[str]], factory.complete)
        messages = history or None
        return await factory_complete(
            prompt=prompt,
            system_prompt=system_prompt or "You are a helpful assistant.",
            model=self.config.model,
            api_key=self.config.api_key,
            base_url=self.config.base_url,
            api_version=getattr(self.config, "api_version", None),
            binding=getattr(self.config, "binding", "openai"),
            reasoning_effort=getattr(self.config, "reasoning_effort", None),
            extra_headers=getattr(self.config, "extra_headers", None),
            messages=messages,
            **kwargs,
        )
    def complete_sync(
        self,
        prompt: str,
        system_prompt: str | None = None,
        history: list[dict[str, str]] | None = None,
        **kwargs: object,
    ) -> str:
        import asyncio

        try:
            asyncio.get_running_loop()
        except RuntimeError:
            return asyncio.run(self.complete(prompt, system_prompt, history, **kwargs))

        raise RuntimeError(
            "LLMClient.complete_sync() cannot be called from a running event loop. "
            "Use `await llm.complete(...)` instead."
        )
        
    
    def get_model_func(self) -> Callable[..., object]:
        return self._build_factory_model_func(allow_multimodal=False)
    
    def get_vision_model_func(self) -> Callable[..., object]:
        return self._build_factory_model_func(allow_multimodal=True)

    def _build_factory_model_func(self, allow_multimodal: bool) -> Callable[..., object]:
        from . import factory

        async def model_func(
            prompt: str,
            system_prompt: str | None = None,
            history_messages: list[dict[str, object]] | None = None,
            image_data: str | None = None,
            messages: list[dict[str, object]] | None = None,
            **kwargs: object,
        ) -> str:
            payload_kwargs: dict[str, object] = dict(kwargs)
            payload_kwargs.pop("history_messages", None)
            payload_kwargs.pop("messages", None)
            payload_kwargs.pop("prompt", None)
            payload_kwargs.pop("system_prompt", None)

            resolved_messages = messages or cast(list[dict[str, object]] | None, history_messages)

            if allow_multimodal and image_data is not None:
                payload_kwargs["image_data"] = image_data

            factory_complete = cast(Callable[..., Awaitable[str]], factory.complete)
            return await factory_complete(
                prompt=prompt,
                system_prompt=system_prompt or "You are a helpful assistant.",
                model=self.config.model,
                api_key=self.config.api_key,
                base_url=sanitize_url(self.config.base_url) if self.config.base_url else None,
                api_version=getattr(self.config, "api_version", None),
                binding=getattr(self.config, "binding", "openai"),
                reasoning_effort=getattr(self.config, "reasoning_effort", None),
                extra_headers=getattr(self.config, "extra_headers", None),
                messages=resolved_messages,
                **payload_kwargs,
            )

        return model_func

_client: LLMClient | None = None


def get_llm_client(config: LLMConfig | None = None) -> LLMClient:
    global _client
    if _client is None:
        _client = LLMClient(config)
    return _client


def reset_llm_client() -> None:
    """Reset the singleton LLM client."""
    global _client
    _client = None
