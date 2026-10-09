import logging
from typing import List
from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger(__name__)

DEFAULT_SECRET_KEY = "mandisamiti-super-secure-production-jwt-key-32chars-min"
MIN_PROD_SECRET_LEN = 32

class Settings(BaseSettings):
    PROJECT_NAME: str = "MandiSamiti Backend API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    APP_ENV: str = "dev"  # "prod" enables the startup secret checks below

    # Security
    SECRET_KEY: str = DEFAULT_SECRET_KEY
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60  # 1 hour
    REFRESH_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 90  # 90 days
    AUTH_PASS_EXPIRE_MINUTES: int = 10
    
    # Database (Default: local async SQLite; In Docker/VPS: PostgreSQL)
    DATABASE_URL: str = "sqlite+aiosqlite:///./mandi_backend.db"
    
    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"
    
    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]
    
    # OTP Configuration
    OTP_EXPIRE_SECONDS: int = 300  # 5 minutes
    OTP_STATIC_ENABLED: bool = True   # Static-code OTP (no SMS provider yet)
    OTP_STATIC_CODE: str = "123456"

    @model_validator(mode="after")
    def _check_prod_secrets(self):
        if self.APP_ENV != "prod":
            return self
        key = self.SECRET_KEY
        if key == DEFAULT_SECRET_KEY or key.startswith("change-me") or len(key) < MIN_PROD_SECRET_LEN:
            raise ValueError(
                "SECRET_KEY is the code default, a change-me placeholder, or shorter than "
                f"{MIN_PROD_SECRET_LEN} chars; refusing to start with APP_ENV=prod. "
                "Set a random key, e.g. `openssl rand -hex 32`."
            )
        if self.OTP_STATIC_ENABLED:
            logger.warning(
                "!!! OTP_STATIC_ENABLED=true in APP_ENV=prod: every phone accepts the static OTP code. "
                "Login security rests on the MPIN only. Switch to an SMS provider before paying shops. !!!"
            )
        return self

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()

