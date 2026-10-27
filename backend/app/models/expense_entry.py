import uuid
import time
from sqlalchemy import Column, String, BigInteger, Integer, ForeignKey
from app.database import Base

class ExpenseEntry(Base):
    __tablename__ = "expense_entries"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    entry_date = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    category = Column(String(20), default="OTHER")  # HAMMALI | KIRAYA | BIJLI | CHAI_PANI | MAJDOORI | OTHER
    amount_paisa = Column(BigInteger, nullable=False, default=0)
    paid_to = Column(String, nullable=True)
    payment_mode = Column(String(20), default="CASH")  # CASH | ONLINE | CHEQUE
    remarks = Column(String, nullable=True)

    is_void = Column(Integer, default=0)
    void_reason = Column(String, nullable=True)
    revision = Column(Integer, default=1)
    is_deleted = Column(Integer, default=0)
    sync_version = Column(Integer, default=1)
    server_seq = Column(BigInteger, nullable=False, default=0, server_default="0", index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000), onupdate=lambda: int(time.time() * 1000))
