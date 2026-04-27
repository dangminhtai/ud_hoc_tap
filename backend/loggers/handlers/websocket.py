import asyncio
import logging
from typing import Optional


class WebSocketLogHandler(logging.Handler):


    def __init__(
        self,
        queue: asyncio.Queue,
        include_module: bool = True,
        service_prefix: Optional[str] = None,
    ):
        super().__init__()
        self.queue = queue
        self.include_module = include_module
        self.service_prefix = service_prefix
        self.setFormatter(logging.Formatter("%(message)s"))

    def emit(self, record: logging.LogRecord):
        """Emit a log record to the queue."""
        try:
            msg = self.format(record)

            # Get display level
            display_level = getattr(record, "display_level", record.levelname)

            # Get module name
            module_name = getattr(record, "module_name", record.name)

            # Build formatted content with standard level tags (compact format)
            level_tag = display_level
            if self.service_prefix:
                service_tag = f"[{self.service_prefix}]"
                if self.include_module:
                    content = f"{service_tag} {level_tag} [{module_name}] {msg}"
                else:
                    content = f"{service_tag} {level_tag} {msg}"
            else:
                if self.include_module:
                    content = f"{level_tag} [{module_name}] {msg}"
                else:
                    content = f"{level_tag} {msg}"

            # Construct structured message
            log_entry = {
                "type": "log",
                "level": display_level,
                "module": module_name,
                "content": content,
                "message": msg,
                "timestamp": record.created,
            }

            # Put into queue non-blocking
            try:
                self.queue.put_nowait(log_entry)
            except asyncio.QueueFull:
                pass  # Drop log if queue is full

        except Exception:
            self.handleError(record)


class LogInterceptor:
    def __init__(
        self,
        logger: logging.Logger,
        queue: asyncio.Queue,
        include_module: bool = True,
    ):
        self.logger = logger
        self.handler = WebSocketLogHandler(queue, include_module)

    def __enter__(self):
        """Attach handler on context enter."""
        self.logger.addHandler(self.handler)
        return self.handler

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Remove handler on context exit."""
        self.logger.removeHandler(self.handler)
