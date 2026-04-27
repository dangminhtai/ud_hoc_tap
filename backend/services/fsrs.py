"""Free Spaced Repetition Scheduling (FSRS) algorithm implementation."""

import math
from datetime import datetime, timedelta
from enum import Enum
from pydantic import BaseModel


class CardRating(str, Enum):
    """Rating scale (1-4)."""
    AGAIN = "again"         # Rating 1 - Again, total forgetting
    HARD = "hard"           # Rating 2 - Hard, difficult to answer
    GOOD = "good"           # Rating 3 - Good, correct answer after some thought
    EASY = "easy"           # Rating 4 - Easy, instant correct response


class FsrsCardState(BaseModel):
    """FSRS card state parameters."""
    due: float                  # Unix timestamp when card is next due
    stability: float            # Memory strength (days)
    difficulty: float          # Card difficulty (1-10)
    elapsed_days: int          # Days since last review
    scheduled_days: int        # Days until next review was scheduled
    reps: int                  # Total repetitions
    lapses: int                # Total lapses (forgetting events)
    state: str                 # "new" | "learning" | "review" | "relearning"
    last_review: float         # Unix timestamp of last review


class FsrsParameters(BaseModel):
    """FSRS algorithm parameters."""
    request_retention: float = 0.9      # Target retention rate (90%)
    maximum_interval: int = 36500       # Max interval in days (100 years)
    weights: list[float] = None         # 19 weights for the algorithm

    def __init__(self, **data):
        super().__init__(**data)
        if self.weights is None:
            # Default weights from research
            self.weights = [
                0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49,
                0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61,
                0.88, 0.147
            ]


class FSRSScheduler:
    """FSRS scheduler for calculating card intervals."""

    def __init__(self, params: FsrsParameters = None):
        self.params = params or FsrsParameters()

    def init_card(self) -> FsrsCardState:
        """Initialize a new card."""
        now = datetime.utcnow().timestamp()
        return FsrsCardState(
            due=now,
            stability=0.0,
            difficulty=5.0,
            elapsed_days=0,
            scheduled_days=0,
            reps=0,
            lapses=0,
            state="new",
            last_review=now
        )

    def schedule(
        self,
        card: FsrsCardState,
        rating: CardRating,
        now: datetime = None
    ) -> FsrsCardState:
        """
        Schedule a card based on user rating.

        Returns updated card state with new due date.
        """
        if now is None:
            now = datetime.utcnow()

        now_ts = now.timestamp()
        last_review_ts = card.last_review

        # Calculate elapsed days since last review
        elapsed_days = max(0, int((now_ts - last_review_ts) / 86400))

        # Update difficulty based on rating
        card.difficulty = self._next_difficulty(card.difficulty, rating)

        if card.state == "new":
            card.reps = 1
            if rating == CardRating.AGAIN:
                card.lapses = 1
                card.state = "learning"
                # Short interval for learning
                interval = 1
            else:
                interval = self._init_interval(rating)
                card.state = "learning" if interval < 7 else "review"

        elif card.state == "learning":
            card.reps += 1
            if rating == CardRating.AGAIN:
                card.lapses += 1
                interval = 1
            else:
                interval = self._init_interval(rating)
                card.state = "review"

        elif card.state == "review":
            card.reps += 1
            if rating == CardRating.AGAIN:
                card.lapses += 1
                card.state = "relearning"
                interval = self._relearning_interval(card)
            else:
                # Calculate interval based on stability and difficulty
                interval = self._next_interval(card, rating)

        elif card.state == "relearning":
            card.reps += 1
            if rating == CardRating.AGAIN:
                interval = 1
            else:
                interval = self._next_interval(card, rating)
                card.state = "review"

        # Apply maximum interval cap
        interval = min(interval, self.params.maximum_interval)

        # Update card state
        card.elapsed_days = elapsed_days
        card.scheduled_days = interval
        card.due = now_ts + (interval * 86400)
        card.last_review = now_ts

        return card

    def _next_difficulty(self, difficulty: float, rating: CardRating) -> float:
        """Calculate next difficulty."""
        delta = {
            CardRating.AGAIN: -1,
            CardRating.HARD: -0.5,
            CardRating.GOOD: 0,
            CardRating.EASY: 1
        }[rating]

        new_difficulty = difficulty + delta
        return max(1.0, min(10.0, new_difficulty))

    def _init_interval(self, rating: CardRating) -> int:
        """Initial interval for new cards."""
        return {
            CardRating.AGAIN: 1,
            CardRating.HARD: 1,
            CardRating.GOOD: 3,
            CardRating.EASY: 7
        }[rating]

    def _relearning_interval(self, card: FsrsCardState) -> int:
        """Interval for relearning cards."""
        return max(1, int(card.scheduled_days / 2))

    def _next_interval(self, card: FsrsCardState, rating: CardRating) -> int:
        """Calculate next interval using FSRS formula."""
        # Simplified FSRS calculation
        factor = {
            CardRating.AGAIN: 0.6,
            CardRating.HARD: 0.8,
            CardRating.GOOD: 1.0,
            CardRating.EASY: 1.3
        }[rating]

        # Base interval calculation
        interval = max(1, int(card.scheduled_days * factor))

        # Apply difficulty modifier (higher difficulty = longer intervals)
        difficulty_factor = 0.5 + (card.difficulty / 10)
        interval = int(interval * difficulty_factor)

        return interval

    def get_interval_days(self, card: FsrsCardState, rating: CardRating) -> int:
        """Preview the interval without updating the card."""
        temp_card = card.copy()
        scheduled = self.schedule(temp_card, rating)
        return scheduled.scheduled_days


# Global scheduler instance
_scheduler = FSRSScheduler()


def get_scheduler() -> FSRSScheduler:
    """Get the global FSRS scheduler."""
    return _scheduler


def init_card() -> FsrsCardState:
    """Initialize a new FSRS card."""
    return _scheduler.init_card()


def schedule_card(
    card: FsrsCardState,
    rating: CardRating,
    now: datetime = None
) -> FsrsCardState:
    """Schedule a card based on user rating."""
    return _scheduler.schedule(card, rating, now)


def get_due_cards_count(cards: list[FsrsCardState], now: datetime = None) -> int:
    """Count how many cards are due for review."""
    if now is None:
        now = datetime.utcnow()
    now_ts = now.timestamp()
    return sum(1 for card in cards if card.due <= now_ts)
