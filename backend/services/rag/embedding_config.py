"""
Embedding optimization configuration.

Controls how embeddings are generated and cached to minimize API calls
and improve performance.
"""

from dataclasses import dataclass
from typing import Optional


@dataclass
class EmbeddingOptimizationConfig:
    """Configuration for embedding optimization."""

    # Chunk size: larger = fewer chunks = fewer embeddings
    # 512 (original): ~2048 chunks per large file → 2048 embeddings
    # 1024 (optimized): ~1024 chunks per large file → 1024 embeddings (50% saving)
    # 2048: ~512 chunks per large file → 512 embeddings (75% saving)
    chunk_size: int = 1024

    # Overlap between chunks (must be < chunk_size)
    # Larger overlap = better context but more redundant chunks
    chunk_overlap: int = 100

    # Enable embedding caching in memory during session
    # Prevents regenerating embeddings for duplicate texts
    enable_embedding_cache: bool = True

    # Use BM25 for initial filtering before dense retrieval
    # BM25 is fast (no embedding) and good for keyword matching
    # Only uses dense embeddings if BM25 score is below threshold
    use_hybrid_retrieval: bool = True

    # BM25 score threshold (0-1)
    # If BM25 similarity >= threshold, skip dense embedding
    bm25_threshold: float = 0.5

    # Batch size for embedding generation
    # Larger batch = faster API calls but more memory
    embedding_batch_size: int = 32

    # Enable adaptive chunking based on document structure
    # Respects sentence/paragraph boundaries instead of hard breaks
    adaptive_chunking: bool = True


# Default optimized config
DEFAULT_OPTIMIZATION = EmbeddingOptimizationConfig(
    chunk_size=1024,  # 50% fewer embeddings than default 512
    chunk_overlap=100,
    enable_embedding_cache=True,
    use_hybrid_retrieval=True,
    bm25_threshold=0.5,
    embedding_batch_size=32,
    adaptive_chunking=True,
)

# Maximum performance config (75% fewer embeddings)
MAXIMUM_PERFORMANCE = EmbeddingOptimizationConfig(
    chunk_size=2048,  # 75% fewer embeddings
    chunk_overlap=200,
    enable_embedding_cache=True,
    use_hybrid_retrieval=True,
    bm25_threshold=0.6,  # More aggressive filtering
    embedding_batch_size=64,
    adaptive_chunking=True,
)

# Balanced config (moderate optimization)
BALANCED = EmbeddingOptimizationConfig(
    chunk_size=1024,
    chunk_overlap=100,
    enable_embedding_cache=True,
    use_hybrid_retrieval=False,  # Standard dense retrieval only
    embedding_batch_size=32,
    adaptive_chunking=True,
)


def get_optimization_config(mode: str = "default") -> EmbeddingOptimizationConfig:
    """Get optimization config by name."""
    configs = {
        "default": DEFAULT_OPTIMIZATION,
        "performance": MAXIMUM_PERFORMANCE,
        "balanced": BALANCED,
    }
    return configs.get(mode, DEFAULT_OPTIMIZATION)
