import re
from pydantic import BaseModel, ConfigDict, field_validator
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

    @field_validator("phone", mode="before")
    @classmethod
    def normalise_phone(cls, v):
        # A bad phone must never fail a whole sync push (the column is String(15)): keep 10 digits or drop it.
        if v is None:
            return None
        digits = re.sub(r"[\s-]", "", str(v)).removeprefix("+")
        if len(digits) == 12 and digits.startswith("91"):
            digits = digits[2:]
        return digits if len(digits) == 10 and digits.isdigit() else None

class PartyCreate(PartyBase):
    pass

class PartyResponse(PartyBase):
    id: str
    shop_id: str
    sync_version: int
    updated_at: int
    created_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)

