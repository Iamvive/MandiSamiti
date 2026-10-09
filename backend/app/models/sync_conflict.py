import uuid
import time
from sqlalchemy import Column, String, Integer, BigInteger, ForeignKey
from app.database import Base

class SyncConflict(Base):
    """A pushed revision whose (entry_id, revision) we already hold with different content. Kept, not applied."""
    __tablename__ = "sync_conflicts"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    shop_id = Column(String, ForeignKey("shop_profiles.id"), nullable=False, index=True)
    entry_id = Column(String, nullable=False)
    revision = Column(Integer, nullable=False)
    snapshot_json = Column(String, nullable=False)
    received_at = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
