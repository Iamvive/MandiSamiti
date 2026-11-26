"""Add analytics_events table for telemetry pipeline.

Revision ID: 0002_analytics_events
Revises: 0001_baseline
Create Date: 2026-11-26

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "0002_analytics_events"
down_revision: Union[str, Sequence[str], None] = "0001_baseline"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "analytics_events",
        sa.Column("shop_id", sa.String(), nullable=False),
        sa.Column("id", sa.String(length=64), nullable=False),
        sa.Column("name", sa.String(length=64), nullable=False),
        sa.Column("screen", sa.String(length=64), nullable=True),
        sa.Column("session_id", sa.String(length=64), nullable=False),
        sa.Column("occurred_at_ms", sa.BigInteger(), nullable=False),
        sa.Column("duration_ms", sa.BigInteger(), nullable=True),
        sa.Column("app_version", sa.String(length=32), nullable=False),
        sa.Column("received_at_ms", sa.BigInteger(), nullable=False),
        sa.ForeignKeyConstraint(["shop_id"], ["shop_profiles.id"]),
        sa.PrimaryKeyConstraint("shop_id", "id"),
    )
    with op.batch_alter_table("analytics_events", schema=None) as batch_op:
        batch_op.create_index(batch_op.f("ix_analytics_events_received_at_ms"), ["received_at_ms"], unique=False)


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("analytics_events", schema=None) as batch_op:
        batch_op.drop_index(batch_op.f("ix_analytics_events_received_at_ms"))

    op.drop_table("analytics_events")
