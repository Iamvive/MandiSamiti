from pydantic import BaseModel

class QuotaResponse(BaseModel):
    plan_tier: str
    bills_used_this_month: int
    monthly_limit: int
    emergency_extensions_used: int
    emergency_extensions_max: int
    emergency_bills_added: int
    effective_limit: int
    can_create_bill: bool
    is_emergency_extension_available: bool
