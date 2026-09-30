from pydantic import BaseModel, ConfigDict
from typing import Optional

class CashTransactionBase(BaseModel):
    id: Optional[str] = None
    party_id: Optional[str] = None
    type: str  # IN, OUT
    amount: float
    category: str = "TRADE_PAYMENT"
    notes: Optional[str] = None
    timestamp: Optional[float] = None

class CashTransactionCreate(CashTransactionBase):
    pass

class CashTransactionResponse(CashTransactionBase):
    id: str
    shop_id: str
    soundbox_broadcasted: int
    sync_version: int
    created_at: float
    updated_at: float

    model_config = ConfigDict(from_attributes=True)
