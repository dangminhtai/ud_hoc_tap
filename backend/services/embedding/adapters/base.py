from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, List, Optional


@dataclass
class EmbeddingRequest:
    texts: List[str]
    model: str
    dimensions: Optional[int] = None
    input_type: Optional[str] = None
    encoding_format: Optional[str] = "float"
    truncate: Optional[bool] = True
    normalized: Optional[bool] = True
    late_chunking: Optional[bool] = False


@dataclass
class EmbeddingResponse:
    embeddings: List[List[float]]
    model: str
    dimensions: int
    usage: Dict[str, Any]


class BaseEmbeddingAdapter(ABC):
    def __init__(self, config: Dict[str, Any]):
        self.api_key = config.get("api_key")
        self.base_url = config.get("base_url")
        self.api_version = config.get("api_version")
        self.model = config.get("model")
        self.dimensions = config.get("dimensions")
        self.request_timeout = config.get("request_timeout", 60)
        self.extra_headers = config.get("extra_headers") or {}

    @abstractmethod
    async def embed(self, request: EmbeddingRequest) -> EmbeddingResponse:
        pass

    @abstractmethod
    def get_model_info(self) -> Dict[str, Any]:
        pass
