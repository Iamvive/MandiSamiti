"""link a cash transaction to the Buy or Sell slip it pays

Revision ID: 0003_cash_tx_source_entry
Revises: 0002_party_tax_fields
Create Date: 2026-11-01
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0003_cash_tx_source_entry"
down_revision: Union[str, Sequence[str], None] = "0002_party_tax_fields"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("cash_transactions") as batch_op:
        batch_op.add_column(sa.Column("source_entry_type", sa.String(length=20), nullable=True))
        batch_op.add_column(sa.Column("source_entry_id", sa.String(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table("cash_transactions") as batch_op:
        batch_op.drop_column("source_entry_id")
        batch_op.drop_column("source_entry_type")
