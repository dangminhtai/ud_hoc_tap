from __future__ import annotations

import asyncio
import time
from types import TracebackType

from backend.loggers import get_logger

logger = get_logger(__name__)

class TrafficController:
    def __init__(
        self,
        provider_name: str,
        max_concurrency: int = 20,
        requests_per_minute: int = 600,
        acquisition_timeout: float = 30.0,
    ) -> None:
        self.provider_name = provider_name
        self.max_concurrency = max_concurrency
        if requests_per_minute <= 0:
            raise ValueError("requests_per_minute must be > 0")
        self.rpm = requests_per_minute
        self.acquisition_timeout = acquisition_timeout
        self._semaphore = asyncio.Semaphore(max_concurrency)
        self._tokens = float(requests_per_minute)
        self._last_refill = time.monotonic()
        self._fill_rate = requests_per_minute / 60.0  
        self._lock = asyncio.Lock()  
        
    async def _wait_for_token(self) -> None:
        async with self._lock:
            now = time.monotonic()
            elapsed = now - self._last_refill

            new_tokens = elapsed * self._fill_rate
            if new_tokens > 0:
                self._tokens = min(float(self.rpm), self._tokens + new_tokens)
                self._last_refill = now

            if self._tokens >= 1:
                self._tokens -= 1.0
                return

            wait_time = (1.0 - self._tokens) / self._fill_rate

        if wait_time > 0:
            logger.debug("[%s] Rate limit active, waiting %.2fs" % (self.provider_name, wait_time))
            await asyncio.sleep(wait_time)
            await self._wait_for_token()

    async def __aenter__(self) -> TrafficController:
        start = time.monotonic()

        try:
            await asyncio.wait_for(self._semaphore.acquire(), timeout=self.acquisition_timeout)
        except TimeoutError:
            logger.error(
                "[%s] Local concurrency limit (%s) exceeded for >%.1fs."
                % (self.provider_name, self.max_concurrency, self.acquisition_timeout)
            )
            raise

        try:
            await self._wait_for_token()
        except Exception:
            self._semaphore.release()
            raise

        wait_duration = time.monotonic() - start
        if wait_duration > 1.0:
            logger.warning("[%s] Traffic control wait: %.2fs" % (self.provider_name, wait_duration))

        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        tb: TracebackType | None,
    ) -> None:
        """Release concurrency slot."""
        self._semaphore.release()
        return None


__all__ = ["TrafficController"]
