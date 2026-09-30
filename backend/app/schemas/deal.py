from pydantic import BaseModel, ConfigDict
from typing import Optional

class DealBase(BaseModel):
    id: Optional[str] = None
    farmer_id: str
    buyer_id: str
    commodity: str
    bags: int = 0
    gross_weight: float
    tare_weight: float = 0.0
    net_weight: float
    rate: float
    commission_rate: float = 0.0
    labour_charges: float = 0.0
    farmer_total: float
    buyer_total: float
    status: str = "COMPLETED"

class DealCreate(DealBase):
    pass

class DealResponse(DealBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: float
    updated_at: float

    model_config = ConfigDict(from_attributes=True)
