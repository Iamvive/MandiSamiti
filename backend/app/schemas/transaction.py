from pydantic import BaseModel, ConfigDict
from typing import Optional

class CashTransactionBase(BaseModel):
    id: Optional[str] = None
    party_id: Optional[str] = None
    deal_id: Optional[str] = None
    transaction_type: str  # UDHAR_GIVEN, JAMA_RECEIVED, INTEREST_ADDED, DISCOUNT_GIVEN
    amount_paisa: int
    payment_mode: str = "CASH"
    category: str = "TRADE_PAYMENT"
    transaction_date: Optional[int] = None
    voice_note_uri: Optional[str] = None
    remarks: Optional[str] = None
    
    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0

class CashTransactionCreate(CashTransactionBase):
    pass

class CashTransactionResponse(CashTransactionBase):
    id: str
    shop_id: str
    soundbox_broadcasted: int
    sync_version: int
    created_at: int
    updated_at: int

    model_config = ConfigDict(from_attributes=True)

