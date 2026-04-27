
import asyncio
from typing import Dict, List, Optional

# Import RAGService as the single entry point
from backend.services.rag.service import RAGService
from backend.services.rag.factory import DEFAULT_PROVIDER as DEFAULT_RAG_PROVIDER


async def rag_search(
    query: str,
    kb_name: Optional[str] = None,
    provider: Optional[str] = None,
    kb_base_dir: Optional[str] = None,
    event_sink=None,
    **kwargs,
) -> dict:
    service = RAGService(kb_base_dir=kb_base_dir, provider=provider)

    try:
        return await service.search(
            query=query,
            kb_name=kb_name,
            event_sink=event_sink,
            **kwargs,
        )
    except Exception as e:
        raise Exception(f"RAG search failed: {e}")


async def initialize_rag(
    kb_name: str,
    documents: List[str],
    provider: Optional[str] = None,
    kb_base_dir: Optional[str] = None,
    **kwargs,
) -> bool:
    service = RAGService(kb_base_dir=kb_base_dir, provider=provider)
    return await service.initialize(kb_name=kb_name, file_paths=documents, **kwargs)


async def delete_rag(
    kb_name: str,
    provider: Optional[str] = None,
    kb_base_dir: Optional[str] = None,
) -> bool:
    service = RAGService(kb_base_dir=kb_base_dir, provider=provider)
    return await service.delete(kb_name=kb_name)


def get_available_providers() -> List[Dict]:
    return RAGService.list_providers()


def get_current_provider() -> str:
    """Get the currently configured RAG provider"""
    return RAGService.get_current_provider()


# Backward compatibility aliases
get_available_plugins = get_available_providers
list_providers = RAGService.list_providers


if __name__ == "__main__":
    import sys

    if sys.platform == "win32":
        import io

        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

    # List available providers
    print("Available RAG Pipelines:")
    for provider in get_available_providers():
        print(f"  - {provider['id']}: {provider['description']}")
    print(f"\nCurrent provider: {get_current_provider()}\n")

    # Test search (requires existing knowledge base)
    result = asyncio.run(
        rag_search(
            "What is the lookup table (LUT) in FPGA?",
            kb_name="DE-all",
        )
    )

    print(f"Query: {result['query']}")
    print(f"Answer: {result['answer']}")
    print(f"Provider: {result.get('provider', 'unknown')}")
