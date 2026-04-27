"""JWT Authentication service."""

from datetime import datetime, timedelta
from typing import Dict, Optional
import jwt
from passlib.context import CryptContext
from pydantic import BaseModel

# JWT configuration
SECRET_KEY = "your-secret-key-change-in-production"  # TODO: Use environment variable
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
REFRESH_TOKEN_EXPIRE_DAYS = 7

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class TokenPayload(BaseModel):
    """JWT token payload."""
    sub: str  # User ID
    exp: int  # Expiration time
    iat: int  # Issued at
    type: str  # "access" | "refresh"


class TokenResponse(BaseModel):
    """Token response."""
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int  # Seconds


class User(BaseModel):
    """User object."""
    id: str
    username: str
    email: str
    is_active: bool = True


def get_password_hash(password: str) -> str:
    """Hash a password."""
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash."""
    return pwd_context.verify(plain_password, hashed_password)


def create_access_token(user_id: str, expires_delta: Optional[timedelta] = None) -> str:
    """Create a JWT access token."""
    if expires_delta is None:
        expires_delta = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)

    now = datetime.utcnow()
    expire = now + expires_delta

    payload = {
        "sub": user_id,
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
        "type": "access"
    }

    encoded_jwt = jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def create_refresh_token(user_id: str) -> str:
    """Create a JWT refresh token."""
    expires_delta = timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)

    now = datetime.utcnow()
    expire = now + expires_delta

    payload = {
        "sub": user_id,
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
        "type": "refresh"
    }

    encoded_jwt = jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def create_tokens(user_id: str) -> TokenResponse:
    """Create both access and refresh tokens."""
    access_token = create_access_token(user_id)
    refresh_token = create_refresh_token(user_id)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )


def verify_token(token: str) -> Optional[TokenPayload]:
    """Verify and decode a JWT token."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        token_data = TokenPayload(**payload)
        return token_data
    except jwt.ExpiredSignatureError:
        return None  # Token expired
    except jwt.InvalidTokenError:
        return None  # Invalid token


def decode_token(token: str) -> Optional[Dict]:
    """Decode a token without verifying signature (for debugging)."""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except Exception:
        return None


# Mock user database (replace with real database)
# Lazy initialization to avoid bcrypt issues at import time
_mock_users = None

def _init_mock_users():
    global _mock_users
    if _mock_users is None:
        _mock_users = {
            "test_user": {
                "id": "test_user",
                "username": "test_user",
                "email": "test@example.com",
                "hashed_password": get_password_hash("password123"),
                "is_active": True
            }
        }
    return _mock_users


def get_user(user_id: str) -> Optional[User]:
    """Get user by ID."""
    users = _init_mock_users()
    user_data = users.get(user_id)
    if user_data:
        return User(
            id=user_data["id"],
            username=user_data["username"],
            email=user_data["email"],
            is_active=user_data["is_active"]
        )
    return None


def authenticate_user(username: str, password: str) -> Optional[User]:
    """Authenticate user with username and password."""
    users = _init_mock_users()
    user_data = None
    for uid, u in users.items():
        if u["username"] == username:
            user_data = u
            break

    if not user_data:
        return None

    if not verify_password(password, user_data["hashed_password"]):
        return None

    return User(
        id=user_data["id"],
        username=user_data["username"],
        email=user_data["email"],
        is_active=user_data["is_active"]
    )


def create_user(username: str, email: str, password: str) -> Optional[User]:
    """Create a new user."""
    users = _init_mock_users()

    # Check if user already exists
    if username in users:
        return None

    user_id = username  # Simplified ID generation
    users[user_id] = {
        "id": user_id,
        "username": username,
        "email": email,
        "hashed_password": get_password_hash(password),
        "is_active": True
    }

    return User(
        id=user_id,
        username=username,
        email=email,
        is_active=True
    )
