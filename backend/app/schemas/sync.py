from pydantic import BaseModel, Field
from typing import List, Optional
from app.schemas.party import PartyCreate, PartyResponse
from app.schemas.deal import DealCreate, DealResponse
from app.schemas.transaction import CashTransactionCreate, CashTransactionResponse
from app.schemas.revision import EntryRevisionCreate, EntryRevisionResponse
from app.schemas.entries import (
    BuyEntryCreate, BuyEntryResponse,
    SellEntryCreate, SellEntryResponse,
    LendEntryCreate, LendEntryResponse,
    LendSettlementCreate, LendSettlementResponse,
    ExpenseEntryCreate, ExpenseEntryResponse,
)

class SyncPushRequest(BaseModel):
    parties: List[PartyCreate] = Field(default_factory=list, max_length=100)
    deals: List[DealCreate] = Field(default_factory=list, max_length=100)
    transactions: List[CashTransactionCreate] = Field(default_factory=list, max_length=100)
    revisions: List[EntryRevisionCreate] = Field(default_factory=list, max_length=100)
    buy_entries: List[BuyEntryCreate] = Field(default_factory=list, max_length=100)
    sell_entries: List[SellEntryCreate] = Field(default_factory=list, max_length=100)
    lend_entries: List[LendEntryCreate] = Field(default_factory=list, max_length=100)
    lend_settlements: List[LendSettlementCreate] = Field(default_factory=list, max_length=100)
    expense_entries: List[ExpenseEntryCreate] = Field(default_factory=list, max_length=100)

class SyncPushResponse(BaseModel):
    success: bool = True
    synced_parties: List[str] = []
    synced_deals: List[str] = []
    synced_transactions: List[str] = []
    synced_revisions: List[str] = []
    synced_buy_entries: List[str] = []
    synced_sell_entries: List[str] = []
    synced_lend_entries: List[str] = []
    synced_lend_settlements: List[str] = []
    synced_expense_entries: List[str] = []
    conflicts: List[str] = []
    server_sync_time: int
    server_seq: int = 0

class SyncPullResponse(BaseModel):
    after_seq: int = 0
    next_seq: int = 0
    has_more: bool = False
    parties: List[PartyResponse] = []
    deals: List[DealResponse] = []
    transactions: List[CashTransactionResponse] = []
    revisions: List[EntryRevisionResponse] = []
    buy_entries: List[BuyEntryResponse] = []
    sell_entries: List[SellEntryResponse] = []
    lend_entries: List[LendEntryResponse] = []
    lend_settlements: List[LendSettlementResponse] = []
    expense_entries: List[ExpenseEntryResponse] = []
    server_sync_time: int
