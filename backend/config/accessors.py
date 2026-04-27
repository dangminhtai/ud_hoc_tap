from typing import Callable


class ConfigAccessor:
    """Config accessor class."""
    def __init__(self, loader: Callable[[], dict]):
        self._loader = loader

    def llm_model(self) -> str:
        """Llm model."""
        cfg = self._loader()
        return str(cfg.get("llm", {}).get("model", "Pro/Flash"))

    def llm_provider(self) -> str:
        """Llm provider."""
        cfg = self._loader()
        return str(cfg.get("llm", {}).get("provider", "openai"))

    def user_data_dir(self) -> str:
        """User data dir."""
        cfg = self._loader()
        return str(cfg.get("paths", {}).get("user_data_dir", "./data/user"))
