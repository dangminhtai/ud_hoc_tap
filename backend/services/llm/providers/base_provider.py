import asyncio
from collections.abc import AsyncIterator
from typing import Callable, Protocol, TypeVar, cast

import anthropic

from ..config import LLMConfig
from ..http_client import get_shared_http_client
from ..registry import register_provider
from ..telemetry import track_llm_call
from ..types import AsyncStreamGenerator, TutorResponse, TutorStreamChunk
from .base_provider import BaseLLMProvider

_DISALLOWED_KWARGS = {
    "api_version",
    "base_url",
    "binding",
    "logit_bias",
    "max_retries",  
    "response_format",
    "seed",
    "stream",
    "stream_options",
}

F = TypeVar("F", bound=Callable[..., object])


class AnthropicDelta(Protocol):
    text: str | None


class AnthropicUsage(Protocol):
    input_tokens: int
    output_tokens: int


class AnthropicChunk(Protocol):
    type: str
    delta: AnthropicDelta
    usage: AnthropicUsage | None


class AnthropicStream(Protocol):
    def __aiter__(self) -> AsyncIterator[AnthropicChunk]: ...


def _coerce_int(value: object, default: int) -> int:
    if isinstance(value, bool):
        return default
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    if isinstance(value, str) and value.isdigit():
        return int(value)
    return default


def _typed_track_llm_call(provider: str) -> Callable[[F], F]:
    return cast(Callable[[F], F], track_llm_call(provider))


def _sanitize_kwargs(kwargs: dict[str, object]) -> dict[str, object]:
    import logging

    sanitized = dict(kwargs)
    removed_keys = []
    for key in _DISALLOWED_KWARGS:
        if key in sanitized:
            removed_keys.append(key)
            sanitized.pop(key)

    if removed_keys:
        logging.getLogger("AnthropicProvider").warning(
            "Ignoring unsupported Anthropic kwargs (handled upstream): %s",
            removed_keys,
        )

    return sanitized


@register_provider("anthropic")
class AnthropicProvider(BaseLLMProvider):

    def __init__(self, config: LLMConfig) -> None:
        super().__init__(config)
        self.client: anthropic.AsyncAnthropic | None = None
        self._client_lock = asyncio.Lock()

    async def _get_client(self) -> anthropic.AsyncAnthropic:
        if self.client is None:
            async with self._client_lock:
                if self.client is None:
                    http_client = await get_shared_http_client()
                    self.client = anthropic.AsyncAnthropic(
                        api_key=self.api_key,
                        http_client=http_client,
                    )
        return self.client

    @_typed_track_llm_call("anthropic")
    async def complete(self, prompt: str, **kwargs: object) -> TutorResponse:
        model_raw = kwargs.pop("model", None)
        model = (
            model_raw
            if isinstance(model_raw, str) and model_raw
            else self.config.model or "claude-3-sonnet-20240229"
        )
        kwargs.pop("max_retries", None)
        kwargs.pop("stream", None)

        async def _call_api() -> TutorResponse:
            """Call api."""
            client = await self._get_client()
            request_kwargs = _sanitize_kwargs(kwargs)
            response = await client.messages.create(  # type: ignore[call-overload]
                model=model,
                max_tokens=_coerce_int(request_kwargs.pop("max_tokens", 1024), 1024),
                messages=[{"role": "user", "content": prompt}],
                **request_kwargs,
            )

            content = response.content[0].text if response.content else ""
            usage = {
                "input_tokens": response.usage.input_tokens,
                "output_tokens": response.usage.output_tokens,
            }

            return TutorResponse(
                content=content,
                raw_response=response.model_dump(),
                usage=usage,
                provider="anthropic",
                model=model,
                finish_reason=response.stop_reason,
                cost_estimate=self.calculate_cost(usage),
            )

        return await self.execute_with_retry(_call_api)

    @_typed_track_llm_call("anthropic")
    def stream(self, prompt: str, **kwargs: object) -> AsyncStreamGenerator:
        model_raw = kwargs.pop("model", None)
        model = (
            model_raw
            if isinstance(model_raw, str) and model_raw
            else self.config.model or "claude-3-sonnet-20240229"
        )
        max_tokens = kwargs.pop("max_tokens", 1024)
        kwargs.pop("max_retries", None)

        async def _create_stream() -> AnthropicStream:
            """Create stream."""
            client = await self._get_client()
            request_kwargs = _sanitize_kwargs(kwargs)
            return cast(
                AnthropicStream,
                await client.messages.create(  # type: ignore[call-overload]
                    model=model,
                    max_tokens=_coerce_int(max_tokens, 1024),
                    messages=[{"role": "user", "content": prompt}],
                    stream=True,
                    **request_kwargs,
                ),
            )

        async def _stream() -> AsyncStreamGenerator:
            """Stream."""
            stream = cast(AnthropicStream, await self.execute_with_retry(_create_stream))
            accumulated_content = ""
            usage = None

            async for chunk in stream:
                if chunk.type == "content_block_delta" and chunk.delta.text:
                    delta = chunk.delta.text
                    accumulated_content += delta

                    yield TutorStreamChunk(
                        content=accumulated_content,
                        delta=delta,
                        provider="anthropic",
                        model=model,
                        is_complete=False,
                    )
                elif chunk.type == "message_delta" and chunk.usage is not None:
                    usage = {
                        "input_tokens": chunk.usage.input_tokens,
                        "output_tokens": chunk.usage.output_tokens,
                    }

            yield TutorStreamChunk(
                content=accumulated_content,
                delta="",
                provider="anthropic",
                model=model,
                is_complete=True,
                usage=usage,
            )

        return _stream()
