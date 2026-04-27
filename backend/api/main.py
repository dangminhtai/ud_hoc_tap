import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi import HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from backend.logging import get_logger
from backend.services.path_service import get_path_service

logger = get_logger("API")


class _ProgressWsAccessFilter(logging.Filter):
    _SUPPRESSED_FRAGMENTS = ("progress/ws", "connection open", "connection closed")

    def filter(self, record: logging.LogRecord) -> bool:
        """Filter."""
        msg = record.getMessage()
        return not any(f in msg for f in self._SUPPRESSED_FRAGMENTS)


for _uv_name in ("uvicorn.access", "uvicorn.error"):
    logging.getLogger(_uv_name).addFilter(_ProgressWsAccessFilter())

CONFIG_DRIFT_ERROR_TEMPLATE = (
    "Configuration Drift Detected: Capability tool references {drift} are not "
    "registered in the runtime tool registry. Register the missing tools or "
    "remove the stale tool names from the capability manifests."
)


class SafeOutputStaticFiles(StaticFiles):
    """Static file mount that only exposes explicitly whitelisted artifacts."""

    def __init__(self, *args, path_service, **kwargs):
        super().__init__(*args, **kwargs)
        self._path_service = path_service

    async def get_response(self, path: str, scope):
        """Return the response."""
        if not self._path_service.is_public_output_path(path):
            raise HTTPException(status_code=404, detail="Output not found")
        return await super().get_response(path, scope)


def validate_tool_consistency():
    """
    Validate that capability manifests only reference tools that are actually
    registered in the runtime ``ToolRegistry``.
    """
    try:
        from backend.runtime.registry.capability_registry import get_capability_registry
        from backend.runtime.registry.tool_registry import get_tool_registry

        capability_registry = get_capability_registry()
        tool_registry = get_tool_registry()
        available_tools = set(tool_registry.list_tools())

        referenced_tools = set()
        for manifest in capability_registry.get_manifests():
            referenced_tools.update(manifest.get("tools_used", []) or [])

        drift = referenced_tools - available_tools
        if drift:
            raise RuntimeError(CONFIG_DRIFT_ERROR_TEMPLATE.format(drift=drift))
    except RuntimeError:
        logger.exception("Configuration validation failed")
        raise
    except Exception:
        logger.exception("Failed to load configuration for validation")
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Execute on startup
    logger.info("Application startup")

    # Validate configuration consistency
    validate_tool_consistency()

    # Initialize LLM client early so OPENAI_* env vars are available before
    # any downstream provider integrations start.
    try:
        from backend.services.llm import get_llm_client

        llm_client = get_llm_client()
        logger.info(f"LLM client initialized: model={llm_client.config.model}")
    except Exception as e:
        logger.warning(f"Failed to initialize LLM client at startup: {e}")

    try:
        from backend.events.event_bus import get_event_bus

        event_bus = get_event_bus()
        await event_bus.start()
        logger.info("EventBus started")
    except Exception as e:
        logger.warning(f"Failed to start EventBus: {e}")

    yield

    # Execute on shutdown
    logger.info("Application shutdown")

    # Stop EventBus
    try:
        from backend.events.event_bus import get_event_bus

        event_bus = get_event_bus()
        await event_bus.stop()
        logger.info("EventBus stopped")
    except Exception as e:
        logger.warning(f"Failed to stop EventBus: {e}")


app = FastAPI(
    title="backend API",
    version="1.0.0",
    lifespan=lifespan,
    redirect_slashes=False,
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

path_service = get_path_service()
user_dir = path_service.get_public_outputs_root()

try:
    from backend.services.setup import init_user_directories

    init_user_directories()
except Exception:
    # Fallback: just create the main directory if it doesn't exist
    if not user_dir.exists():
        user_dir.mkdir(parents=True)

app.mount(
    "/api/outputs",
    SafeOutputStaticFiles(directory=str(user_dir), path_service=path_service),
    name="outputs",
)

from backend.api.routers import (
    chat,
    flashcard_gen,
    knowledge,
    question,
    sessions,
    settings,
    system,
    unified_ws,
)

# Include routers
app.include_router(chat.router, prefix="/api/v1", tags=["chat"])
app.include_router(question.router, prefix="/api/v1/question", tags=["question"])
app.include_router(knowledge.router, prefix="/api/v1/knowledge", tags=["knowledge"])
app.include_router(flashcard_gen.router, prefix="/api/v1", tags=["flashcard"])
app.include_router(sessions.router, prefix="/api/v1/sessions", tags=["sessions"])
app.include_router(settings.router, prefix="/api/v1/settings", tags=["settings"])
app.include_router(system.router, prefix="/api/v1/system", tags=["system"])
app.include_router(unified_ws.router, prefix="/api/v1", tags=["unified-ws"])


@app.get("/")
async def root():
    """Root."""
    return {"message": "Welcome to backend API"}


if __name__ == "__main__":
    from backend.api.run_server import main as run_server_main

    run_server_main()
