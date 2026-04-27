from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Protocol

@dataclass
class ToolParameter:
    name: str
    type: str  # "string" | "integer" | "boolean" | "number" | "array" | "object"
    description: str = ""
    required: bool = True
    default: Any = None
    enum: list[str] | None = None
    
    
    def to_schema(self) -> dict[str, Any]:
        schema: dict[str, Any] = {"type": self.type, "description": self.description}
        if self.enum:
            schema["enum"] = self.enum
        return schema

@dataclass
class ToolDefinition:
    name: str
    description: str
    parameters: list[ToolParameter] = field(default_factory=list)
    
    def to_openai_schema(self) -> dict[str, Any]:
        properties = {}
        required = []
        for p in self.parameters:
            properties[p.name] = p.to_schema()
            if p.required:
                required.append(p.name)
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": {
                    "type": "object",
                    "properties": properties,
                    "required": required,
                },
            },
        }
        
@dataclass
class ToolAlias:
    name: str
    description: str = ""
    input_format: str = ""
    when_to_use: str = ""
    phase: str = ""


@dataclass
class ToolPromptHints:
    
    short_description: str = ""
    when_to_use: str = ""
    input_format: str = ""
    guideline: str = ""
    note: str = ""
    phase: str = ""
    aliases: list[ToolAlias] = field(default_factory=list)


@dataclass
class ToolResult:
    content: str = ""
    sources: list[dict[str, Any]] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    success: bool = True

    def __str__(self) -> str:
        return self.content
    
class ToolEventSink(Protocol):
    async def __call__(
        self,
        event_type: str,
        message: str = "",
        metadata: dict[str, Any] | None = None,
    ) -> None: ...


class BaseTool(ABC):
    @abstractmethod
    def get_definition(self) -> ToolDefinition:
        ...

    @abstractmethod
    async def execute(self, **kwargs: Any) -> ToolResult:
        ...

    def get_prompt_hints(self, language: str = "en") -> ToolPromptHints:
        definition = self.get_definition()
        return ToolPromptHints(
            short_description=definition.description,
        )

    @property
    def name(self) -> str:
        return self.get_definition().name