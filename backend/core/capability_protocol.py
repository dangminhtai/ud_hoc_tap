from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any

from .context import UnifiedContext
from .stream_bus import StreamBus

@dataclass
class CapabilityManifest:
    name: str
    description: str
    stages: list[str] = field(default_factory=list)
    tools_used: list[str] = field(default_factory=list)
    cli_aliases: list[str] = field(default_factory=list)
    request_schema: dict[str, Any] = field(default_factory=dict)
    config_defaults: dict[str, Any] = field(default_factory=dict)
    
class BaseCapability(ABC):
    manifest: CapabilityManifest
    @abstractmethod
    async def run(self, context: UnifiedContext, stream: StreamBus) -> None:
        ...
    
    @property
    def name(self) -> str:
        return self.manifest.name

    @property
    def stages(self) -> list[str]:
        return self.manifest.stages
