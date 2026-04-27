"""Smart reminder system for due cards."""

from datetime import datetime, timedelta
from fastapi import APIRouter, HTTPException, Request, Query
from pydantic import BaseModel

router = APIRouter()


class ReminderSettingsDto(BaseModel):
    """User reminder settings."""
    enabled: bool = True
    reminder_time: str = "08:00"  # HH:MM format
    reminders_per_day: int = 3
    min_cards_threshold: int = 1
    notify_only_critical: bool = False  # Only notify about weak spots
    quiet_hours_start: str | None = None  # HH:MM format
    quiet_hours_end: str | None = None


class ReminderDto(BaseModel):
    """A single reminder notification."""
    id: str
    deck_id: str
    deck_name: str
    due_cards_count: int
    critical_cards_count: int
    scheduled_time: float
    created_at: float
    sent_at: float | None = None


@router.get("/settings", response_model=ReminderSettingsDto)
async def get_reminder_settings(request: Request):
    """Get user's reminder settings."""
    try:
        # Mock settings
        return ReminderSettingsDto(
            enabled=True,
            reminder_time="08:00",
            reminders_per_day=1,
            min_cards_threshold=5,
            notify_only_critical=False,
            quiet_hours_start="22:00",
            quiet_hours_end="06:00"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching reminder settings: {str(e)}")


@router.put("/settings", response_model=ReminderSettingsDto)
async def update_reminder_settings(request: Request, settings: ReminderSettingsDto):
    """Update user's reminder settings."""
    try:
        # TODO: Save reminder settings to database
        # Validate:
        # - reminder_time is valid HH:MM
        # - quiet_hours are valid if provided
        # - reminders_per_day is reasonable (1-10)

        return settings
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating reminder settings: {str(e)}")


@router.get("/pending", response_model=list[ReminderDto])
async def get_pending_reminders(request: Request):
    """Get pending reminders for the user."""
    try:
        # Mock pending reminders
        return [
            ReminderDto(
                id="rem1",
                deck_id="deck1",
                deck_name="Toán cao cấp",
                due_cards_count=15,
                critical_cards_count=3,
                scheduled_time=datetime.utcnow().timestamp() + 3600,
                created_at=datetime.utcnow().timestamp()
            )
        ]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching pending reminders: {str(e)}")


@router.post("/send-now/{deck_id}")
async def send_reminder_now(request: Request, deck_id: str):
    """Send reminder for a specific deck immediately."""
    try:
        # TODO: Generate and queue reminder for sending
        # Used for manual reminder triggers

        return {
            "status": "sent",
            "deck_id": deck_id,
            "timestamp": datetime.utcnow().timestamp()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error sending reminder: {str(e)}")


@router.post("/schedule-reminder")
async def schedule_reminder(request: Request, body: ReminderDto):
    """Schedule a reminder for a specific time."""
    try:
        # TODO: Save reminder to queue
        # Process by background scheduler

        return {
            "reminder_id": "",
            "status": "scheduled",
            "scheduled_time": body.scheduled_time
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error scheduling reminder: {str(e)}")


@router.get("/history")
async def get_reminder_history(
    request: Request,
    days: int = Query(30, ge=1, le=365),
    limit: int = Query(20, ge=1, le=100)
):
    """Get history of sent reminders."""
    try:
        return {
            "period_days": days,
            "total_reminders": 10,
            "engagement_rate": 0.8,
            "reminders": [
                {
                    "date": "2026-04-26",
                    "deck": "Tiếng Anh",
                    "cards": 20,
                    "reviewed": True
                }
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching reminder history: {str(e)}")


@router.post("/test")
async def test_reminder(request: Request):
    """Send a test reminder to verify push notification setup."""
    try:
        # TODO: Send immediate test reminder
        # Useful for verifying FCM/push notification configuration

        return {
            "status": "test_sent",
            "message": "Check your device for a test notification"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error sending test reminder: {str(e)}")


# Background scheduler functions
async def schedule_daily_reminders():
    """Background task to check and send reminders (called by scheduler)."""
    # TODO: This would be called by APScheduler or similar
    # 1. Get all users with reminders enabled
    # 2. For each user:
    #    - Check settings
    #    - Check quiet hours
    #    - Get pending decks
    #    - Queue reminders for sending
    pass


async def process_reminder_queue():
    """Background task to process and send queued reminders."""
    # TODO: Send reminders via FCM/push notification service
    pass
