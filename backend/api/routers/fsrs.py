"""FSRS (Free Spaced Repetition Scheduling) endpoints."""

import json
from datetime import datetime
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from backend.services.fsrs import FSRSScheduler, FsrsCardState, CardRating, get_scheduler
from backend.api.rate_limiter import limiter

router = APIRouter()


class CardStateDto(BaseModel):
    """Card state DTO."""
    card_id: str
    due: float
    stability: float
    difficulty: float
    elapsed_days: int
    scheduled_days: int
    reps: int
    lapses: int
    state: str
    last_review: float


class ScheduleCardRequest(BaseModel):
    """Request to schedule a card."""
    card_state: dict  # Serialized FsrsCardState
    rating: str      # "again" | "hard" | "good" | "easy"


class ScheduleCardResponse(BaseModel):
    """Response with scheduled card state."""
    card_state: dict
    interval_days: int
    due_timestamp: float


class GetDueCardsRequest(BaseModel):
    """Request to get due cards count."""
    cards: list[dict]  # Serialized FsrsCardState list


class GetDueCardsResponse(BaseModel):
    """Response with due cards count."""
    total_cards: int
    due_cards: int
    due_percentage: float


@router.post("/schedule-card", response_model=ScheduleCardResponse)
@limiter.limit("500/hour")
async def schedule_card(request: Request, body: ScheduleCardRequest):
    """Schedule a card based on user rating."""
    try:
        # Reconstruct FsrsCardState from dict
        card_data = body.card_state
        card = FsrsCardState(
            due=card_data.get("due", 0),
            stability=card_data.get("stability", 0),
            difficulty=card_data.get("difficulty", 5),
            elapsed_days=card_data.get("elapsed_days", 0),
            scheduled_days=card_data.get("scheduled_days", 0),
            reps=card_data.get("reps", 0),
            lapses=card_data.get("lapses", 0),
            state=card_data.get("state", "new"),
            last_review=card_data.get("last_review", datetime.utcnow().timestamp())
        )

        # Validate rating
        try:
            rating = CardRating(body.rating)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid rating: {body.rating}")

        # Schedule the card
        scheduler = get_scheduler()
        scheduled = scheduler.schedule(card, rating)

        return ScheduleCardResponse(
            card_state=json.loads(scheduled.json()),
            interval_days=scheduled.scheduled_days,
            due_timestamp=scheduled.due
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error scheduling card: {str(e)}")


@router.post("/get-due-cards", response_model=GetDueCardsResponse)
@limiter.limit("200/hour")
async def get_due_cards(request: Request, body: GetDueCardsRequest):
    """Count due cards for review."""
    try:
        # Reconstruct FsrsCardState objects from dicts
        cards = []
        for card_data in body.cards:
            card = FsrsCardState(
                due=card_data.get("due", 0),
                stability=card_data.get("stability", 0),
                difficulty=card_data.get("difficulty", 5),
                elapsed_days=card_data.get("elapsed_days", 0),
                scheduled_days=card_data.get("scheduled_days", 0),
                reps=card_data.get("reps", 0),
                lapses=card_data.get("lapses", 0),
                state=card_data.get("state", "new"),
                last_review=card_data.get("last_review", datetime.utcnow().timestamp())
            )
            cards.append(card)

        # Get scheduler and count due cards
        scheduler = get_scheduler()
        now = datetime.utcnow()
        now_ts = now.timestamp()

        total = len(cards)
        due = sum(1 for card in cards if card.due <= now_ts)

        return GetDueCardsResponse(
            total_cards=total,
            due_cards=due,
            due_percentage=round((due / total * 100) if total > 0 else 0, 2)
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error getting due cards: {str(e)}")


@router.post("/init-card")
@limiter.limit("1000/hour")
async def init_card(request: Request):
    """Initialize a new FSRS card."""
    try:
        scheduler = get_scheduler()
        card = scheduler.init_card()
        return json.loads(card.json())
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error initializing card: {str(e)}")


@router.post("/preview-interval")
@limiter.limit("500/hour")
async def preview_interval(request: Request, body: ScheduleCardRequest):
    """Preview interval without updating card state."""
    try:
        # Reconstruct card state
        card_data = body.card_state
        card = FsrsCardState(
            due=card_data.get("due", 0),
            stability=card_data.get("stability", 0),
            difficulty=card_data.get("difficulty", 5),
            elapsed_days=card_data.get("elapsed_days", 0),
            scheduled_days=card_data.get("scheduled_days", 0),
            reps=card_data.get("reps", 0),
            lapses=card_data.get("lapses", 0),
            state=card_data.get("state", "new"),
            last_review=card_data.get("last_review", datetime.utcnow().timestamp())
        )

        # Validate rating
        try:
            rating = CardRating(body.rating)
        except ValueError:
            raise HTTPException(status_code=400, detail=f"Invalid rating: {body.rating}")

        # Get preview
        scheduler = get_scheduler()
        interval = scheduler.get_interval_days(card, rating)

        return {
            "rating": body.rating,
            "interval_days": interval,
            "preview_due": datetime.utcnow().timestamp() + (interval * 86400)
        }

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error previewing interval: {str(e)}")
