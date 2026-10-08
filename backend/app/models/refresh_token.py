import time
from sqlalchemy import Column, String, BigInteger, ForeignKey
from app.database import Base


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    jti = Column(String, primary_key=True)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    expires_at = Column(BigInteger, nullable=False)  # ms
    revoked_at = Column(BigInteger, nullable=True)   # ms
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
