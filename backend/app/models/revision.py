import uuid
import time
from sqlalchemy import Column, String, Integer, BigInteger, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.database import Base

class EntryRevision(Base):
    __tablename__ = "entry_revisions"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    entry_id = Column(String, nullable=False, index=True)
    entry_kind = Column(String(20), nullable=False)  # DEAL | CASH
    revision = Column(Integer, nullable=False)
    change_kind = Column(String(20), nullable=False)  # CREATE | EDIT | VOID
    snapshot_json = Column(String, nullable=False)
    void_reason = Column(String, nullable=True)
    changed_at = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)

    __table_args__ = (
        UniqueConstraint("entry_id", "revision", name="uq_entry_revision"),
    )
