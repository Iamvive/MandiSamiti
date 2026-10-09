from pydantic import BaseModel, ConfigDict
from typing import Optional

class PartyBase(BaseModel):
    id: Optional[str] = None
    name: str
    phone: Optional[str] = None
    role: str  # FARMER, BUYER
    village: Optional[str] = None
    monthly_interest_rate: Optional[float] = 1.5
    photo_uri: Optional[str] = None
    is_deleted: int = 0
    updated_at: Optional[int] = None

class PartyCreate(PartyBase):
    pass

class PartyResponse(PartyBase):
    id: str
    shop_id: str
    sync_version: int
    updated_at: int
    created_at: int

    model_config = ConfigDict(from_attributes=True)

