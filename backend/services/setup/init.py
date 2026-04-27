from __future__ import annotations

from pathlib import Path

from backend.services.config.env_store import EnvStore


def get_backend_port(project_root: Path | None = None) -> int:
    """Return the configured backend port (default 8001)."""
    store = EnvStore() if project_root is None else EnvStore(project_root / ".env")
    return store.as_summary().backend_port


def get_frontend_port(project_root: Path | None = None) -> int:
    """Return the configured frontend port (default 3782)."""
    store = EnvStore() if project_root is None else EnvStore(project_root / ".env")
    return store.as_summary().frontend_port


def get_ports(project_root: Path | None = None) -> dict[str, int]:
    """Return both backend and frontend ports."""
    store = EnvStore() if project_root is None else EnvStore(project_root / ".env")
    summary = store.as_summary()
    return {"backend": summary.backend_port, "frontend": summary.frontend_port}


def init_user_directories() -> None:
    """Create required user data directories if they do not already exist."""
    from backend.services.path_service import get_path_service

    path_service = get_path_service()
    dirs = [
        path_service.get_public_outputs_root(),
        path_service.get_user_root(),
    ]
    for d in dirs:
        d.mkdir(parents=True, exist_ok=True)
