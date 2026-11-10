"""add quota and emergency extension fields to shop_profiles

Revision ID: 0004_shop_quota
Revises: 0003_cash_tx_source_entry
Create Date: 2026-11-10
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0004_shop_quota"
down_revision: Union[str, Sequence[str], None] = "0003_cash_tx_source_entry"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("shop_profiles") as batch_op:
        batch_op.add_column(
            sa.Column("monthly_bill_limit", sa.Integer(), nullable=False, server_default="50")
        )
        batch_op.add_column(
            sa.Column("emergency_extensions_used", sa.Integer(), nullable=False, server_default="0")
        )
        batch_op.add_column(
            sa.Column("emergency_extension_bills", sa.Integer(), nullable=False, server_default="0")
        )
        batch_op.add_column(
            sa.Column("plan_tier", sa.String(length=32), nullable=False, server_default="FREE")
        )
        batch_op.add_column(
            sa.Column("plan_valid_until", sa.DateTime(timezone=True), nullable=True)
        )


def downgrade() -> None:
    with op.batch_alter_table("shop_profiles") as batch_op:
        batch_op.drop_column("plan_valid_until")
        batch_op.drop_column("plan_tier")
        batch_op.drop_column("emergency_extension_bills")
        batch_op.drop_column("emergency_extensions_used")
        batch_op.drop_column("monthly_bill_limit")
