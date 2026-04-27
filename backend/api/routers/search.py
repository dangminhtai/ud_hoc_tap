"""Full-text search for flashcards and knowledge bases."""

from fastapi import APIRouter, HTTPException, Request, Query
from pydantic import BaseModel

router = APIRouter()


class SearchResultItem(BaseModel):
    """Single search result item."""
    type: str  # "flashcard" | "kb_item"
    id: str
    title: str
    content: str
    deck_id: str | None  # For flashcards
    kb_name: str | None  # For KB items
    score: float  # Relevance score 0-1
    highlighted: str  # HTML with <mark> tags


class SearchResponse(BaseModel):
    """Search response."""
    query: str
    results: list[SearchResultItem]
    total_count: int
    execution_time_ms: float


class SearchFilters(BaseModel):
    """Optional search filters."""
    deck_id: str | None = None
    kb_name: str | None = None
    type: str | None = None  # "flashcard" | "kb_item"
    min_score: float = 0.3


@router.get("/flashcards", response_model=SearchResponse)
async def search_flashcards(
    request: Request,
    q: str = Query(..., min_length=1, max_length=500),
    deck_id: str | None = None,
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0)
):
    """
    Search flashcards using full-text search.

    Searches both question and answer text.
    Returns results ranked by relevance.
    """
    try:
        import time
        start_time = time.time()
        
        # Mock data
        mock_results = [
            SearchResultItem(
                type="flashcard",
                id="fc1",
                title="What is RAG?",
                content="Retrieval-Augmented Generation is a technique that...",
                deck_id="deck1",
                kb_name=None,
                score=0.95,
                highlighted="What is <mark>RAG</mark>?"
            )
        ] if q.lower() in "rag" else []

        return SearchResponse(
            query=q,
            results=mock_results,
            total_count=len(mock_results),
            execution_time_ms=(time.time() - start_time) * 1000
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search error: {str(e)}")


@router.get("/knowledge-base", response_model=SearchResponse)
async def search_knowledge_base(
    request: Request,
    q: str = Query(..., min_length=1, max_length=500),
    kb_name: str | None = None,
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0)
):
    """
    Search knowledge base items.

    Searches content indexed from PDFs, TXT files, and web sources.
    """
    try:
        import time
        start_time = time.time()
        
        # Mock data
        mock_results = [
            SearchResultItem(
                type="kb_item",
                id="kb1",
                title="Introduction to Machine Learning",
                content="Machine learning is a subset of artificial intelligence...",
                deck_id=None,
                kb_name=kb_name or "default_kb",
                score=0.88,
                highlighted="Introduction to <mark>Machine Learning</mark>"
            )
        ] if len(q) > 2 else []

        return SearchResponse(
            query=q,
            results=mock_results,
            total_count=len(mock_results),
            execution_time_ms=(time.time() - start_time) * 1000
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search error: {str(e)}")


@router.get("/all", response_model=SearchResponse)
async def search_all(
    request: Request,
    q: str = Query(..., min_length=1, max_length=500),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0)
):
    """
    Search across all content (flashcards + knowledge base).

    Returns mixed results ranked by relevance and type priority.
    Priority: flashcards > kb_items
    """
    try:
        import time
        start_time = time.time()
        
        # Mock data combining both
        mock_results = [
            SearchResultItem(
                type="flashcard",
                id="fc1",
                title="Concept",
                content=f"Mock flashcard containing {q}",
                deck_id="deck1",
                kb_name=None,
                score=0.9,
                highlighted=f"<mark>{q}</mark>"
            ),
            SearchResultItem(
                type="kb_item",
                id="kb1",
                title="Document",
                content=f"Mock document containing {q}",
                deck_id=None,
                kb_name="kb1",
                score=0.85,
                highlighted=f"<mark>{q}</mark>"
            )
        ]

        return SearchResponse(
            query=q,
            results=mock_results,
            total_count=len(mock_results),
            execution_time_ms=(time.time() - start_time) * 1000
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search error: {str(e)}")


@router.get("/suggestions")
async def search_suggestions(
    request: Request,
    q: str = Query(..., min_length=1, max_length=100),
    limit: int = Query(5, ge=1, le=20)
):
    """
    Get search suggestions/autocomplete.

    Returns common search terms and recent searches.
    """
    try:
        return {
            "suggestions": [f"{q} basics", f"{q} advanced"],
            "recent": [q, "previous search"],
            "popular": ["python", "machine learning", "fastapi"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Suggestion error: {str(e)}")


@router.post("/index-rebuild")
async def rebuild_search_index(request: Request):
    """Rebuild the full-text search index."""
    try:
        # TODO: Rebuild FTS index for all flashcards and KB items
        # This should be run:
        # - On app startup
        # - After bulk imports
        # - Periodically for maintenance

        return {
            "status": "success",
            "indexed_items": 0,
            "index_size_mb": 0
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Indexing error: {str(e)}")
