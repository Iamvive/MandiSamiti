from typing import Protocol
from fastapi import HTTPException
from app.config import settings


class OtpSender(Protocol):
    async def send(self, phone: str, code: str) -> None: ...


class StaticOtpSender:
    def __init__(self, code: str):
        self.code = code

    async def send(self, phone: str, code: str) -> None:
        return None


def get_otp_sender() -> OtpSender:
    if settings.OTP_STATIC_ENABLED:
        return StaticOtpSender(settings.OTP_STATIC_CODE)
    raise HTTPException(status_code=503, detail="OTP service not configured")
