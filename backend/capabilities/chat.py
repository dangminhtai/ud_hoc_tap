"""Agentic chat capability."""

from __future__ import annotations

from backend.core.capability_protocol import BaseCapability, CapabilityManifest
from backend.core.context import UnifiedContext
from backend.core.stream_bus import StreamBus
from backend.agents.chat.agentic_pipeline import CHAT_OPTIONAL_TOOLS, AgenticChatPipeline
from backend.capabilities.request_contracts import get_capability_request_schema


class ChatCapability(BaseCapability):
    """Chat capability class."""
    manifest = CapabilityManifest(
        name="chat",
        description="Agentic chat with autonomous tool selection across enabled tools.",
        stages=["thinking", "acting", "observing", "responding"],
        tools_used=CHAT_OPTIONAL_TOOLS,
        cli_aliases=["chat"],
        request_schema=get_capability_request_schema("chat"),
    )

    async def run(self, context: UnifiedContext, stream: StreamBus) -> None:
        """Run."""
        pipeline = AgenticChatPipeline(language=context.language)
        await pipeline.run(context, stream)
