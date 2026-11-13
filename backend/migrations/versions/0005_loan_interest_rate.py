"""per-loan monthly interest rate, default 2

Revision ID: 0005_loan_interest_rate
Revises: 0004_shop_quota
Create Date: 2026-11-13
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0005_loan_interest_rate"
down_revision: Union[str, Sequence[str], None] = "0004_shop_quota"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.add_column(
            sa.Column("monthly_interest_rate", sa.Float(), nullable=False, server_default="2.0")
        )
    op.execute("UPDATE shop_profiles SET default_monthly_interest_rate = 2.0 WHERE default_monthly_interest_rate = 1.5")
    op.execute("UPDATE parties SET monthly_interest_rate = 2.0 WHERE monthly_interest_rate = 1.5")


def downgrade() -> None:
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.drop_column("monthly_interest_rate")
