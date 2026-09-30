import os
from typing import List

class Settings:
    PROJECT_NAME: str = "MandiSamiti Backend API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Security
    SECRET_KEY: str = os.getenv("SECRET_KEY", "mandisamiti-super-secure-production-jwt-key-32chars-min")
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days
    REFRESH_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30  # 30 days
    
    # Database
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER", "localhost")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "postgres")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "mandisamiti")
    POSTGRES_PORT: str = os.getenv("POSTGRES_PORT", "5432")
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        f"postgresql+asyncpg://{POSTGRES_USER}:{POSTGRES_PASSWORD}@{POSTGRES_SERVER}:{POSTGRES_PORT}/{POSTGRES_DB}"
    )
    
    # Redis
    REDIS_URL: str = os.getenv("REDIS_URL", "redis://localhost:6379/0")
    
    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]
    
    # OTP Configuration
    OTP_EXPIRE_SECONDS: int = 300  # 5 minutes
    OTP_MOCK_MODE: bool = True     # Returns OTP in response in dev/test mode

settings = Settings()
