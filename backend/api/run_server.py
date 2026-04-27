import asyncio
import os
from pathlib import Path
import sys

if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsProactorEventLoopPolicy())

import uvicorn

os.environ["PYTHONUNBUFFERED"] = "1"
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(line_buffering=True)
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(line_buffering=True)

def main() -> None:
    project_root = Path(__file__).parent.parent.parent

    os.chdir(str(project_root))

    if str(project_root) not in sys.path:
        sys.path.insert(0, str(project_root))

    from backend.services.setup import get_backend_port

    backend_port = get_backend_port(project_root)

    reload_excludes = [
        str(project_root / "venv"),  
        str(project_root / ".venv"),  
        str(project_root / "data"), 
        str(project_root / "node_modules"),  
        str(project_root / "web" / "node_modules"), 
        str(project_root / "web" / ".next"),  
        str(project_root / ".git"), 
        str(project_root / "scripts"),
    ]

    reload_excludes = [d for d in reload_excludes if Path(d).exists()]

    enable_reload = os.environ.get("backend_NO_RELOAD", "0") != "1"

    uvicorn.run(
        "backend.api.main:app",
        host="0.0.0.0",
        port=backend_port,
        reload=enable_reload,
        reload_excludes=reload_excludes if enable_reload else [],
        log_level="info",
    )


if __name__ == "__main__":
    main()
