import time
from sqlalchemy import BigInteger, Column, ForeignKey, String
from app.database import Base


class AnalyticsEvent(Base):
    """Clickstream and sync telemetry event uploaded from the mobile app."""

    __tablename__ = "analytics_events"

    shop_id = Column(String, ForeignKey("shop_profiles.id"), primary_key=True, nullable=False)
    id = Column(String(64), primary_key=True, nullable=False)
    name = Column(String(64), nullable=False)
    screen = Column(String(64), nullable=True)
    session_id = Column(String(64), nullable=False)
    occurred_at_ms = Column(BigInteger, nullable=False)
    duration_ms = Column(BigInteger, nullable=True)
    app_version = Column(String(32), nullable=False)
    received_at_ms = Column(
        BigInteger,
        default=lambda: int(time.time() * 1000),
        nullable=False,
        index=True,
    )
