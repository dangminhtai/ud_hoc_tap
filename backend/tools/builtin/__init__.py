"""Built-in tool implementations and metadata."""

from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

from backend.core.tool_protocol import BaseTool, ToolDefinition, ToolParameter, ToolResult
from backend.tools.prompting import load_prompt_hints

logger = logging.getLogger(__name__)


class _PromptHintsMixin:
    """Shared prompt-hint loader for built-in tools."""

    def get_prompt_hints(self, language: str = "en"):
        """Return the prompt hints."""
        return load_prompt_hints(self.name, language=language)


class BrainstormTool(_PromptHintsMixin, BaseTool):
    """Brainstorm tool class."""
    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="brainstorm",
            description="Broadly explore multiple possibilities for a topic and give a short rationale for each.",
            parameters=[
                ToolParameter(
                    name="topic",
                    type="string",
                    description="The topic, goal, or problem to brainstorm about.",
                ),
                ToolParameter(
                    name="context",
                    type="string",
                    description="Optional supporting context, constraints, or background.",
                    required=False,
                ),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.brainstorm import brainstorm

        result = await brainstorm(
            topic=kwargs.get("topic", ""),
            context=kwargs.get("context", ""),
            api_key=kwargs.get("api_key"),
            base_url=kwargs.get("base_url"),
            model=kwargs.get("model"),
            max_tokens=kwargs.get("max_tokens"),
            temperature=kwargs.get("temperature"),
        )
        return ToolResult(content=result.get("answer", ""), metadata=result)


class RAGTool(_PromptHintsMixin, BaseTool):
    """R a g tool class."""
    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="rag",
            description=(
                "Search a knowledge base using Retrieval-Augmented Generation. "
                "Returns relevant passages and an LLM-synthesised answer."
            ),
            parameters=[
                ToolParameter(name="query", type="string", description="Search query."),
                ToolParameter(
                    name="kb_name",
                    type="string",
                    description="Knowledge base to search.",
                    required=False,
                ),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.rag_tool import rag_search

        query = kwargs.get("query", "")
        kb_name = kwargs.get("kb_name")
        event_sink = kwargs.get("event_sink")
        extra_kwargs = {
            key: value
            for key, value in kwargs.items()
            if key not in {"query", "kb_name", "mode", "event_sink"}
        }

        result = await rag_search(
            query=query,
            kb_name=kb_name,
            event_sink=event_sink,
            **extra_kwargs,
        )
        content = result.get("answer") or result.get("content", "")
        return ToolResult(
            content=content,
            sources=[{"type": "rag", "query": query, "kb_name": kb_name}],
            metadata=result,
        )


class WebSearchTool(_PromptHintsMixin, BaseTool):
    """Web search tool class."""
    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="web_search",
            description="Search the web and return summarised results with citations.",
            parameters=[
                ToolParameter(name="query", type="string", description="Search query."),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.web_search import web_search

        query = kwargs.get("query", "")
        output_dir = kwargs.get("output_dir")
        verbose = kwargs.get("verbose", False)
        result = await asyncio.to_thread(
            web_search,
            query=query,
            output_dir=output_dir,
            verbose=verbose,
        )

        if isinstance(result, dict):
            answer = result.get("answer", "")
            citations = result.get("citations", [])
        else:
            answer = str(result)
            citations = []

        return ToolResult(
            content=answer,
            sources=[
                {"type": "web", "url": citation.get("url", ""), "title": citation.get("title", "")}
                for citation in citations
            ],
            metadata=result if isinstance(result, dict) else {"raw": answer},
        )


class CodeExecutionTool(_PromptHintsMixin, BaseTool):
    """Code execution tool class."""
    _CODEGEN_SYSTEM_PROMPT = """You are a Python code generator.

Convert the user's natural-language request into executable Python code only.

Rules:
- Output only Python code, with no markdown fences or explanation.
- Prefer standard library plus these common packages when useful: math, numpy, pandas, matplotlib, scipy, sympy.
- Print the final answer to stdout.
- Save plots or generated files to the current working directory.
- Keep the code focused on the requested computation or verification task.
"""

    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="code_execution",
            description="Turn a natural-language computation request into Python, run it in a restricted Python worker, and return the result.",
            parameters=[
                ToolParameter(
                    name="intent",
                    type="string",
                    description="Natural-language description of the computation or verification task.",
                ),
                ToolParameter(
                    name="code",
                    type="string",
                    description="Optional raw Python code to execute directly.",
                    required=False,
                ),
                ToolParameter(
                    name="timeout",
                    type="integer",
                    description="Max execution time in seconds.",
                    required=False,
                    default=30,
                ),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.code_executor import run_code

        code = str(kwargs.get("code") or "").strip()
        intent = str(kwargs.get("intent") or kwargs.get("query") or "").strip()
        timeout = int(kwargs.get("timeout", 30) or 30)
        workspace_dir = kwargs.get("workspace_dir")
        feature = kwargs.get("feature")
        task_id = kwargs.get("task_id")
        session_id = kwargs.get("session_id")
        turn_id = kwargs.get("turn_id")

        if not code:
            if not intent:
                raise ValueError("code_execution requires either 'intent' or 'code'")
            code = await self._generate_code(intent)

        result = await run_code(
            language="python",
            code=code,
            timeout=timeout,
            workspace_dir=workspace_dir,
            feature=feature,
            task_id=task_id,
            session_id=session_id,
            turn_id=turn_id,
        )
        stdout = result.get("stdout", "")
        stderr = result.get("stderr", "")
        exit_code = result.get("exit_code", 1)
        artifacts = result.get("artifacts", [])

        parts: list[str] = []
        if stdout:
            parts.append(stdout.strip())
        if stderr:
            label = "Error" if exit_code else "Stderr"
            parts.append(f"{label}:\n{stderr.strip()}")
        if artifacts:
            parts.append(f"Artifacts: {', '.join(str(item) for item in artifacts)}")
        if not parts:
            parts.append("Execution completed with no output.")

        metadata = {**result, "code": code, "intent": intent}
        return ToolResult(
            content="\n\n".join(parts),
            success=exit_code == 0,
            sources=[{"type": "code", "file": artifact} for artifact in artifacts],
            metadata=metadata,
        )

    async def _generate_code(self, intent: str) -> str:
        """Generate code."""
        from backend.services.llm import complete, get_token_limit_kwargs
        from backend.services.llm.config import get_llm_config

        llm_config = get_llm_config()
        completion_kwargs: dict[str, Any] = {"temperature": 0.0}
        if getattr(llm_config, "model", None):
            completion_kwargs.update(get_token_limit_kwargs(llm_config.model, 1200))

        response = await complete(
            prompt=intent,
            system_prompt=self._CODEGEN_SYSTEM_PROMPT,
            model=llm_config.model,
            api_key=llm_config.api_key,
            base_url=llm_config.base_url,
            api_version=getattr(llm_config, "api_version", None),
            binding=getattr(llm_config, "binding", None),
            **completion_kwargs,
        )
        code = self._strip_markdown_fences(response)
        if not code.strip():
            raise ValueError("LLM returned empty code for code_execution")
        return code

    @staticmethod
    def _strip_markdown_fences(content: str) -> str:
        """Strip markdown fences."""
        cleaned = content.strip()
        if not cleaned.startswith("```"):
            return cleaned

        lines = cleaned.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        return "\n".join(lines).strip()


class ReasonTool(_PromptHintsMixin, BaseTool):
    """Reason tool class."""
    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="reason",
            description=(
                "Perform deep reasoning on a complex sub-problem using a dedicated LLM call. "
                "Use when the current context is insufficient for a confident answer."
            ),
            parameters=[
                ToolParameter(
                    name="query",
                    type="string",
                    description="The sub-problem to reason about.",
                ),
                ToolParameter(
                    name="context",
                    type="string",
                    description="Supporting context for reasoning.",
                    required=False,
                ),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.reason import reason

        result = await reason(
            query=kwargs.get("query", ""),
            context=kwargs.get("context", ""),
            api_key=kwargs.get("api_key"),
            base_url=kwargs.get("base_url"),
            model=kwargs.get("model"),
            max_tokens=kwargs.get("max_tokens"),
            temperature=kwargs.get("temperature"),
        )
        return ToolResult(content=result.get("answer", ""), metadata=result)


class PaperSearchToolWrapper(_PromptHintsMixin, BaseTool):
    """Paper search tool wrapper class."""
    def get_definition(self) -> ToolDefinition:
        """Return the definition."""
        return ToolDefinition(
            name="paper_search",
            description="Search arXiv preprints by keyword and return concise metadata.",
            parameters=[
                ToolParameter(name="query", type="string", description="Search query."),
                ToolParameter(
                    name="max_results",
                    type="integer",
                    description="Maximum papers to return.",
                    required=False,
                    default=3,
                ),
                ToolParameter(
                    name="years_limit",
                    type="integer",
                    description="Only include preprints from the last N years.",
                    required=False,
                    default=3,
                ),
                ToolParameter(
                    name="sort_by",
                    type="string",
                    description="Sort by relevance or submission date.",
                    required=False,
                    default="relevance",
                    enum=["relevance", "date"],
                ),
            ],
        )

    async def execute(self, **kwargs: Any) -> ToolResult:
        """Execute."""
        from backend.tools.paper_search_tool import ArxivSearchTool

        try:
            papers = await ArxivSearchTool().search_papers(
                query=kwargs.get("query", ""),
                max_results=kwargs.get("max_results", 3),
                years_limit=kwargs.get("years_limit", 3),
                sort_by=kwargs.get("sort_by", "relevance"),
            )
        except Exception:
            return ToolResult(
                content="arXiv search is temporarily unavailable (rate-limited or network error). Please try again later.",
                sources=[],
                metadata={"provider": "arxiv", "papers": [], "error": True},
            )
        if not papers:
            return ToolResult(
                content="No arXiv preprints found for this query.",
                sources=[],
                metadata={"provider": "arxiv", "papers": []},
            )

        lines: list[str] = []
        for paper in papers:
            lines.append(f"**{paper['title']}** ({paper.get('year', '?')})")
            lines.append(f"Authors: {', '.join(paper.get('authors', []))}")
            lines.append(f"arXiv: {paper.get('arxiv_id', '')}")
            lines.append(f"URL: {paper.get('url', '')}")
            lines.append(f"Abstract: {paper.get('abstract', '')[:400]}")
            lines.append("")

        return ToolResult(
            content="\n".join(lines),
            sources=[
                {
                    "type": "paper",
                    "provider": "arxiv",
                    "url": paper.get("url", ""),
                    "title": paper.get("title", ""),
                    "arxiv_id": paper.get("arxiv_id", ""),
                }
                for paper in papers
            ],
            metadata={"provider": "arxiv", "papers": papers},
        )


BUILTIN_TOOL_TYPES: tuple[type[BaseTool], ...] = (
    BrainstormTool,
    RAGTool,
    WebSearchTool,
    CodeExecutionTool,
    ReasonTool,
    PaperSearchToolWrapper,
)

BUILTIN_TOOL_NAMES: tuple[str, ...] = tuple(tool_type().name for tool_type in BUILTIN_TOOL_TYPES)

TOOL_ALIASES: dict[str, tuple[str, dict[str, Any]]] = {
    "rag_hybrid": ("rag", {}),
    "rag_naive": ("rag", {}),
    "rag_search": ("rag", {}),
    "code_execute": ("code_execution", {}),
    "run_code": ("code_execution", {}),
}

__all__ = [
    "BUILTIN_TOOL_NAMES",
    "BUILTIN_TOOL_TYPES",
    "TOOL_ALIASES",
    "BrainstormTool",
    "CodeExecutionTool",
    "PaperSearchToolWrapper",
    "RAGTool",
    "ReasonTool",
    "WebSearchTool",
]
