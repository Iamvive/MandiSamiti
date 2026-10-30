from pydantic import BaseModel, ConfigDict
from typing import Optional


class BuyEntryBase(BaseModel):
    id: Optional[str] = None
    farmer_id: str
    commodity_id: str
    entry_date: Optional[int] = None

    bags_count: int = 0
    weight_per_bag_grams: int = 0
    extra_weight_grams: int = 0
    total_weight_grams: int = 0
    rate_paisa_per_quintal: int = 0
    commission_bps: int = 0
    labour_per_bag_paisa: int = 0
    advance_paid_paisa: int = 0
    gross_paisa: int = 0
    commission_paisa: int = 0
    labour_paisa: int = 0
    net_payable_paisa: int = 0
    paid_paisa: int = 0

    vehicle_number: Optional[str] = None
    slip_number: Optional[str] = None
    slip_no: Optional[int] = None
    remarks: Optional[str] = None

    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0


class BuyEntryCreate(BuyEntryBase):
    pass


class BuyEntryResponse(BuyEntryBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)


class SellEntryBase(BaseModel):
    id: Optional[str] = None
    buyer_id: str
    commodity_id: str
    entry_date: Optional[int] = None
    bill_mode: str = "NORMAL"
    party_kind: str = "MERCHANT"
    gstin: Optional[str] = None

    bags_count: int = 0
    weight_per_bag_grams: int = 0
    total_weight_grams: int = 0
    rate_paisa_per_quintal: int = 0
    arat_bps: int = 0
    tulak_paisa: int = 0
    gross_paisa: int = 0
    arat_paisa: int = 0
    mandi_shulk_paisa: int = 0
    total_receivable_paisa: int = 0
    received_paisa: int = 0
    balance_due_paisa: int = 0

    vehicle_number: Optional[str] = None
    invoice_number: Optional[str] = None
    slip_no: Optional[int] = None
    remarks: Optional[str] = None

    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0


class SellEntryCreate(SellEntryBase):
    pass


class SellEntryResponse(SellEntryBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)


class LendEntryBase(BaseModel):
    id: Optional[str] = None
    party_id: str
    lend_type: str = "MONEY"
    entry_date: Optional[int] = None
    amount_paisa: int = 0
    commodity_id: Optional[str] = None
    bags_count: int = 0
    weight_grams: int = 0
    expected_return_date: Optional[int] = None
    slip_no: Optional[int] = None
    notes: Optional[str] = None
    status: str = "OPEN"

    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0


class LendEntryCreate(LendEntryBase):
    pass


class LendEntryResponse(LendEntryBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)


class LendSettlementBase(BaseModel):
    id: Optional[str] = None
    lend_id: str
    settled_at: Optional[int] = None
    amount_paisa: int = 0
    principal_paisa: int = 0
    interest_paisa: int = 0
    calculated_interest_paisa: int = 0
    weight_grams: int = 0
    bags_count: int = 0

    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1


class LendSettlementCreate(LendSettlementBase):
    pass


class LendSettlementResponse(LendSettlementBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)


class ExpenseEntryBase(BaseModel):
    id: Optional[str] = None
    entry_date: Optional[int] = None
    category: str = "OTHER"
    amount_paisa: int = 0
    paid_to: Optional[str] = None
    payment_mode: str = "CASH"
    remarks: Optional[str] = None

    is_void: int = 0
    void_reason: Optional[str] = None
    revision: int = 1
    is_deleted: int = 0


class ExpenseEntryCreate(ExpenseEntryBase):
    pass


class ExpenseEntryResponse(ExpenseEntryBase):
    id: str
    shop_id: str
    sync_version: int
    created_at: int
    updated_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)
