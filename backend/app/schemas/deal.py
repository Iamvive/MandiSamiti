from pydantic import BaseModel, ConfigDict
from typing import Optional

class DealBase(BaseModel):
    id: Optional[str] = None
    farmer_id: str
    buyer_id: Optional[str] = None
    commodity: str
    deal_status: str = "SETTLED"  # PENDING_SETTLEMENT, SETTLED, CANCELLED
    deal_date: Optional[int] = None
    
    # Weight (Grams)
    bags_count: int = 0
    gross_weight_grams: int = 0
    cut_weight_grams: int = 0
    net_weight_grams: int = 0

    # Pricing & Deductions (Paisa)
    rate_paisa_per_unit: int = 0
    gross_amount_paisa: int = 0
    farmer_commission_bps: int = 0
    farmer_commission_paisa: int = 0
    buyer_commission_paisa: int = 0
    labour_charge_paisa: int = 0
    weighing_charge_paisa: int = 0
    other_deductions_paisa: int = 0

    # Settlements (Paisa)
    net_farmer_payable_paisa: int = 0
    net_buyer_receivable_paisa: int = 0

    receipt_photo_uri: Optional[str] = None
    voice_note_uri: Optional[str] = None
    remarks: Optional[str] = None
    
    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0

class DealCreate(DealBase):
    pass

class DealResponse(DealBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)

