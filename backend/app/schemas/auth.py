from typing import Optional
from pydantic import BaseModel, Field

Phone = Field(..., description="10 digit Indian mobile number", examples=["9876543210"])
Mpin = Field(..., pattern=r"^\d{4}$", examples=["1234"])


class CheckPhoneRequest(BaseModel):
    phone: str = Phone


class CheckPhoneResponse(BaseModel):
    is_new_user: bool


class SignupRequest(BaseModel):
    phone: str = Phone
    shop_name: str = Field(..., min_length=1, max_length=120)
    owner_name: str = Field(..., min_length=1, max_length=120)
    mandi_name: str = Field(..., min_length=1, max_length=120)
    mpin: str = Mpin


class LoginRequest(BaseModel):
    phone: str = Phone
    mpin: str = Mpin


class RefreshRequest(BaseModel):
    refresh_token: str


class LogoutRequest(BaseModel):
    refresh_token: str


class ShopOut(BaseModel):
    id: str
    shop_name: str
    owner_name: Optional[str] = None
    mandi_name: Optional[str] = None
    phone_number: Optional[str] = None


class AuthSession(BaseModel):
    access_token: str
    refresh_token: str
    shop: ShopOut


class RefreshResponse(BaseModel):
    access_token: str
    refresh_token: str
