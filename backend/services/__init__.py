from .path_service import PathService, get_path_service

__all__ = [
    "llm",
    "embedding",
    "rag",
    "prompt",
    "search",
    "setup",
    "session",
    "config",
    "PathService",
    "get_path_service",
    "BaseSessionManager",
]


def __getattr__(name: str):
    """Lazy import for modules that depend on heavy libraries."""
    import importlib

    if name == "llm":
        return importlib.import_module(f"{__name__}.llm")
    if name == "prompt":
        return importlib.import_module(f"{__name__}.prompt")
    if name == "search":
        return importlib.import_module(f"{__name__}.search")
    if name == "setup":
        return importlib.import_module(f"{__name__}.setup")
    if name == "session":
        return importlib.import_module(f"{__name__}.session")
    if name == "config":
        return importlib.import_module(f"{__name__}.config")
    if name == "rag":
        return importlib.import_module(f"{__name__}.rag")
    if name == "embedding":
        return importlib.import_module(f"{__name__}.embedding")
    if name == "BaseSessionManager":
        from .session import BaseSessionManager

        return BaseSessionManager
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
