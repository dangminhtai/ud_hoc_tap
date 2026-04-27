from __future__ import annotations

import asyncio
import logging
import uuid
from typing import Any, AsyncIterator


from backend.core.context import UnifiedContext
from backend.core.stream import StreamEvent, StreamEventType
from backend.core.stream_bus import StreamBus
from backend.events.event_bus import Event, EventType, get_event_bus
from backend.runtime.registry.capability_registry import get_capability_registry
from backend.runtime.registry.tool_registry import get_tool_registry

logger = logging.getLogger(__name__)

class ChatOrchestrator:
    def __init__(self) -> None:
        self._cap_registry = get_capability_registry()
        self._tool_registry = get_tool_registry()
        
    async def handle(self, context: UnifiedContext) -> AsyncIterator[StreamEvent]:
        if not context.session_id:
            context.session_id = str(uuid.uuid4())
        
        cap_name = context.active_capability or "chat"
        capability = self._cap_registry.get(cap_name)
        
        if capability is None:
            bus = StreamBus()
            await bus.error(
                f"Unknown capability: {cap_name}. "
                f"Available: {self._cap_registry.list_capabilities()}",
                source="orchestrator",
            )
            await bus.close()
            async for event in bus.subscribe():
                yield event
            return
        yield StreamEvent(
            type=StreamEventType.SESSION,
            source="orchestrator",
            metadata={
                "session_id": context.session_id,
                "turn_id": str(context.metadata.get("turn_id", "")),
            },
        )

        bus = StreamBus()
        async def _run() -> None:
            try:
                await capability.run(context, bus)
            except Exception as exc:
                logger.error("Capability %s failed: %s", cap_name, exc, exc_info=True)
                await bus.error(str(exc), source=cap_name)
            finally:
                await bus.emit(StreamEvent(type=StreamEventType.DONE, source=cap_name))
                await bus.close()
        
        stream = bus.subscribe()
        task = asyncio.create_task(_run())
        
        async for event in stream:
            yield event

        await task
        await self._publish_completion(context, cap_name)
        
    async def _publish_completion(self, context: UnifiedContext, cap_name: str) -> None:
        try:
            bus = get_event_bus()
            await bus.publish(
                Event(
                    type=EventType.CAPABILITY_COMPLETE,
                    task_id=str(context.metadata.get("turn_id") or context.session_id),
                    user_input=context.user_message,
                    agent_output="",
                    metadata={
                        "capability": cap_name,
                        "session_id": context.session_id,
                        "turn_id": str(context.metadata.get("turn_id", "")),
                    },
                )
            )
        except Exception:
            logger.debug("EventBus publish failed (may not be running)", exc_info=True)
            
    def list_tools(self) -> list[str]:
        """Return a list of tools."""
        return self._tool_registry.list_tools()

    def list_capabilities(self) -> list[str]:
        """Return a list of capabilities."""
        return self._cap_registry.list_capabilities()

    def get_capability_manifests(self) -> list[dict[str, Any]]:
        """Return the capability manifests."""
        return self._cap_registry.get_manifests()

    def get_tool_schemas(self, names: list[str] | None = None) -> list[dict[str, Any]]:
        """Return the tool schemas."""
        return self._tool_registry.build_openai_schemas(names)
