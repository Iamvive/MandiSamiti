from pydantic import BaseModel, ConfigDict
from typing import Optional

class PartyBase(BaseModel):
    id: Optional[str] = None
    name: str
    phone: Optional[str] = None
    role: str  # FARMER, BUYER
    village: Optional[str] = None
    current_balance: float = 0.0

class PartyCreate(PartyBase):
    pass

class PartyResponse(PartyBase):
    id: str
    shop_id: str
    sync_version: int
    updated_at: float
    created_at: float

    model_config = ConfigDict(from_attributes=True)
