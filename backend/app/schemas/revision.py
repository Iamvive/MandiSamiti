from pydantic import BaseModel, ConfigDict
from typing import Optional

class EntryRevisionBase(BaseModel):
    id: Optional[str] = None
    entry_id: str
    entry_kind: str  # DEAL | CASH
    revision: int
    change_kind: str  # CREATE | EDIT | VOID
    snapshot_json: str
    void_reason: Optional[str] = None
    changed_at: Optional[int] = None

class EntryRevisionCreate(EntryRevisionBase):
    pass

class EntryRevisionResponse(EntryRevisionBase):
    id: str
    shop_id: str
    changed_at: int
    server_seq: int = 0

    model_config = ConfigDict(from_attributes=True)
