"""Authentication endpoints."""

from fastapi import APIRouter, HTTPException, Request, Depends, Header
from pydantic import BaseModel, EmailStr
from datetime import datetime
from typing import Optional

from backend.services.auth import (
    authenticate_user,
    create_user,
    create_tokens,
    verify_token,
    get_user,
    User,
    TokenResponse
)

router = APIRouter()


def get_token_from_header(authorization: Optional[str] = Header(None)) -> str:
    """Extract Bearer token from Authorization header."""
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing authorization header")

    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(status_code=401, detail="Invalid authorization header")

    return parts[1]


class LoginRequest(BaseModel):
    """Login request."""
    username: str
    password: str


class SignupRequest(BaseModel):
    """Signup request."""
    username: str
    email: EmailStr
    password: str


class TokenRefreshRequest(BaseModel):
    """Token refresh request."""
    refresh_token: str


@router.post("/login", response_model=TokenResponse)
async def login(request: Request, body: LoginRequest):
    """
    Login with username and password.

    Returns access and refresh tokens.
    """
    try:
        user = authenticate_user(body.username, body.password)
        if not user:
            raise HTTPException(status_code=401, detail="Invalid username or password")

        tokens = create_tokens(user.id)
        return tokens

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Login error: {str(e)}")


@router.post("/signup", response_model=TokenResponse)
async def signup(request: Request, body: SignupRequest):
    """
    Register a new user.

    Returns access and refresh tokens.
    """
    try:
        # TODO: Check if username/email already exists
        # TODO: Validate password strength

        user = create_user(body.username, body.email, body.password)
        if not user:
            raise HTTPException(status_code=400, detail="Failed to create user")

        tokens = create_tokens(user.id)
        return tokens

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Signup error: {str(e)}")


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(request: Request, body: TokenRefreshRequest):
    """
    Refresh an expired access token using a refresh token.

    Returns new access and refresh tokens.
    """
    try:
        # Verify refresh token
        token_data = verify_token(body.refresh_token)
        if not token_data or token_data.type != "refresh":
            raise HTTPException(status_code=401, detail="Invalid refresh token")

        # Create new tokens
        tokens = create_tokens(token_data.sub)
        return tokens

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Token refresh error: {str(e)}")


@router.get("/me", response_model=User)
async def get_current_user(
    request: Request,
    token: str = Depends(get_token_from_header)
):
    """
    Get current user information.

    Requires valid access token in Authorization header: Bearer <token>
    """
    try:
        # Verify access token
        token_data = verify_token(token)
        if not token_data or token_data.type != "access":
            raise HTTPException(status_code=401, detail="Invalid access token")

        user = get_user(token_data.sub)
        if not user:
            raise HTTPException(status_code=404, detail="User not found")

        return user

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Auth error: {str(e)}")


@router.post("/logout")
async def logout(request: Request, token: str = Depends(get_token_from_header)):
    """
    Logout user.

    Invalidates the current token (client should discard it).
    In a production system, this would add the token to a blacklist.
    """
    try:
        token_data = verify_token(token)
        if not token_data:
            raise HTTPException(status_code=401, detail="Invalid token")

        # TODO: Add token to blacklist in Redis or database
        # This prevents the token from being used again

        return {"status": "logged_out"}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Logout error: {str(e)}")


@router.post("/verify-token")
async def verify_access_token(
    request: Request,
    token: str = Depends(get_token_from_header)
):
    """
    Verify if an access token is valid.

    Used by client to check token validity before making requests.
    """
    try:
        token_data = verify_token(token)
        if not token_data or token_data.type != "access":
            return {"valid": False}

        return {
            "valid": True,
            "user_id": token_data.sub,
            "expires_at": token_data.exp
        }

    except Exception:
        return {"valid": False}
