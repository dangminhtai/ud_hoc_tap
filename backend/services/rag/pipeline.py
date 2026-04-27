"""
RAG Pipeline
============

Composable RAG pipeline with fluent API.
"""

import asyncio
from pathlib import Path
import shutil
from typing import Any, Dict, List, Optional

from backend.logging import get_logger

from .components.base import Component
from .components.routing import FileTypeRouter
from .types import Document

# Default knowledge base directory
DEFAULT_KB_BASE_DIR = str(
    Path(__file__).resolve().parent.parent.parent.parent / "data" / "knowledge_bases"
)


class RAGPipeline:
    def __init__(self, name: str = "default", kb_base_dir: Optional[str] = None):
        self.name = name
        self.kb_base_dir = kb_base_dir or DEFAULT_KB_BASE_DIR
        self.logger = get_logger(f"Pipeline:{name}")
        self._parser: Optional[Component] = None
        self._chunkers: List[Component] = []
        self._embedder: Optional[Component] = None
        self._indexers: List[Component] = []
        self._retriever: Optional[Component] = None

    def parser(self, p: Component) -> "RAGPipeline":
        self._parser = p
        return self

    def chunker(self, c: Component) -> "RAGPipeline":
        self._chunkers.append(c)
        return self

    def embedder(self, e: Component) -> "RAGPipeline":
        self._embedder = e
        return self

    def indexer(self, i: Component) -> "RAGPipeline":
        """Add an indexer to the pipeline."""
        self._indexers.append(i)
        return self

    def retriever(self, r: Component) -> "RAGPipeline":
        """Set the retriever."""
        self._retriever = r
        return self

    async def initialize(self, kb_name: str, file_paths: List[str], **kwargs) -> bool:
        self.logger.info(f"Initializing KB '{kb_name}' with {len(file_paths)} files")

        if not self._parser:
            raise ValueError("No parser configured. Use .parser() to set one")

        # Stage 1: Parse documents with file type routing
        self.logger.info("Stage 1: Parsing documents...")

        # Classify files by type
        classification = FileTypeRouter.classify_files(file_paths)
        self.logger.info(
            f"File classification: {len(classification.parser_files)} parser, "
            f"{len(classification.text_files)} text, "
            f"{len(classification.unsupported)} unsupported"
        )

        documents = []

        # Process complex files (PDF, etc.) with configured parser
        for path in classification.parser_files:
            self.logger.info(f"Parsing (parser): {Path(path).name}")
            doc = await self._parser.process(path, **kwargs)
            documents.append(doc)

        # Process text files directly (fast path)
        for path in classification.text_files:
            self.logger.info(f"Parsing (direct text): {Path(path).name}")
            content = await FileTypeRouter.read_text_file(path)
            doc = Document(
                content=content,
                file_path=str(path),
                metadata={
                    "filename": Path(path).name,
                    "parser": "direct_text",
                },
            )
            documents.append(doc)

        # Log unsupported files
        for path in classification.unsupported:
            self.logger.warning(f"Skipped unsupported file: {Path(path).name}")

        # Stage 2: Chunk (sequential - later chunkers see earlier results)
        if self._chunkers:
            self.logger.info("Stage 2: Chunking...")
            for chunker in self._chunkers:
                for doc in documents:
                    new_chunks = await chunker.process(doc, **kwargs)
                    doc.chunks.extend(new_chunks)

        # Stage 3: Embed
        if self._embedder:
            self.logger.info("Stage 3: Embedding...")
            for doc in documents:
                await self._embedder.process(doc, **kwargs)

        # Stage 4: Index (can run in parallel)
        if self._indexers:
            self.logger.info("Stage 4: Indexing...")
            await asyncio.gather(
                *[indexer.process(kb_name, documents, **kwargs) for indexer in self._indexers]
            )

        self.logger.info(f"KB '{kb_name}' initialized successfully")
        return True

    async def search(self, query: str, kb_name: str, **kwargs) -> Dict[str, Any]:
        if not self._retriever:
            raise ValueError("No retriever configured. Use .retriever() to set one")

        return await self._retriever.process(query, kb_name=kb_name, **kwargs)

    async def delete(self, kb_name: str) -> bool:
        if not kb_name or kb_name in (".", "..") or "/" in kb_name or "\\" in kb_name:
            raise ValueError(f"Invalid knowledge base name: {kb_name}")

        self.logger.info(f"Deleting KB '{kb_name}'")

        kb_dir = Path(self.kb_base_dir) / kb_name
        kb_dir = kb_dir.resolve()
        base_dir = Path(self.kb_base_dir).resolve()
        if not kb_dir.is_relative_to(base_dir):
            raise ValueError(f"Knowledge base path outside allowed directory: {kb_name}")

        if kb_dir.exists():
            shutil.rmtree(kb_dir)
            self.logger.info(f"Deleted KB directory: {kb_dir}")
            return True

        self.logger.warning(f"KB directory not found: {kb_dir}")
        return False
