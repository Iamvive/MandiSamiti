import os
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "MandiSamiti Backend API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Security
    SECRET_KEY: str = "mandisamiti-super-secure-production-jwt-key-32chars-min"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days
    REFRESH_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30  # 30 days
    
    # Database (Default: local async SQLite; In Docker/VPS: PostgreSQL)
    DATABASE_URL: str = "sqlite+aiosqlite:///./mandi_backend.db"
    
    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"
    
    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]
    
    # OTP Configuration
    OTP_EXPIRE_SECONDS: int = 300  # 5 minutes
    OTP_MOCK_MODE: bool = True     # Returns OTP in response in dev/test mode

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()

