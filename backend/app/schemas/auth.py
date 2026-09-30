from pydantic import BaseModel, Field
from typing import Optional

class SendOTPRequest(BaseModel):
    phone_number: str = Field(..., description="10 digit Indian mobile number", example="9876543210")

class SendOTPResponse(BaseModel):
    success: bool
    message: str
    phone_number: str
    expires_in_seconds: int = 300
    mock_otp: Optional[str] = None  # Returned only in dev / test mode

class VerifyOTPRequest(BaseModel):
    phone_number: str = Field(..., example="9876543210")
    otp: str = Field(..., min_length=4, max_length=6, example="123456")
    shop_name: Optional[str] = Field(None, example="श्री गणेश ट्रेडिंग")
    user_name: Optional[str] = Field(None, example="Vivek Ji")

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user_id: str
    shop_id: Optional[str] = None
    role: str
    user_name: str
    shop_name: Optional[str] = None

class SetupMPINRequest(BaseModel):
    mpin: str = Field(..., min_length=4, max_length=6, example="1234")

class VerifyMPINRequest(BaseModel):
    phone_number: str = Field(..., example="9876543210")
    mpin: str = Field(..., min_length=4, max_length=6, example="1234")

class RefreshTokenRequest(BaseModel):
    refresh_token: str
