import time
from typing import Optional, Any
import jwt
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from passlib.context import CryptContext
from app.config import settings

security_bearer = HTTPBearer()

_pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")

def get_password_hash(password: str) -> str:
    return _pwd.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        return _pwd.verify(plain_password, hashed_password)
    except ValueError:
        return False

def create_access_token(subject: str, shop_id: Optional[str] = None, role: str = "OWNER") -> str:
    expire = time.time() + (settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60)
    to_encode = {
        "sub": subject,
        "shop_id": shop_id,
        "role": role,
        "exp": int(expire),
        "type": "access"
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def create_refresh_token(subject: str) -> str:
    expire = time.time() + (settings.REFRESH_TOKEN_EXPIRE_MINUTES * 60)
    to_encode = {
        "sub": subject,
        "exp": int(expire),
        "type": "refresh"
    }
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def decode_token(token: str) -> dict[str, Any]:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        return payload
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials or token expired",
            headers={"WWW-Authenticate": "Bearer"},
        )

async def get_current_user_payload(credentials: HTTPAuthorizationCredentials = Security(security_bearer)) -> dict[str, Any]:
    token = credentials.credentials
    payload = decode_token(token)
    if payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid access token type",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return payload
