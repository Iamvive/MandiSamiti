from pydantic import BaseModel
from typing import List, Optional
from app.schemas.party import PartyCreate, PartyResponse
from app.schemas.deal import DealCreate, DealResponse
from app.schemas.transaction import CashTransactionCreate, CashTransactionResponse
from app.schemas.revision import EntryRevisionCreate, EntryRevisionResponse

class SyncPushRequest(BaseModel):
    parties: List[PartyCreate] = []
    deals: List[DealCreate] = []
    transactions: List[CashTransactionCreate] = []
    revisions: List[EntryRevisionCreate] = []

class SyncPushResponse(BaseModel):
    success: bool = True
    synced_parties: List[str] = []
    synced_deals: List[str] = []
    synced_transactions: List[str] = []
    synced_revisions: List[str] = []
    server_sync_time: int
    server_seq: int = 0

class SyncPullResponse(BaseModel):
    last_sync_timestamp: int = 0
    after_seq: int = 0
    next_seq: int = 0
    has_more: bool = False
    parties: List[PartyResponse] = []
    deals: List[DealResponse] = []
    transactions: List[CashTransactionResponse] = []
    revisions: List[EntryRevisionResponse] = []
    server_sync_time: int

