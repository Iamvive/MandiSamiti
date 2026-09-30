from pydantic import BaseModel
from typing import List, Optional
from app.schemas.party import PartyCreate, PartyResponse
from app.schemas.deal import DealCreate, DealResponse
from app.schemas.transaction import CashTransactionCreate, CashTransactionResponse

class SyncPushRequest(BaseModel):
    parties: List[PartyCreate] = []
    deals: List[DealCreate] = []
    transactions: List[CashTransactionCreate] = []

class SyncPushResponse(BaseModel):
    success: bool
    synced_parties: List[str]
    synced_deals: List[str]
    synced_transactions: List[str]
    server_sync_time: float

class SyncPullResponse(BaseModel):
    last_sync_timestamp: float
    parties: List[PartyResponse]
    deals: List[DealResponse]
    transactions: List[CashTransactionResponse]
    server_sync_time: float
