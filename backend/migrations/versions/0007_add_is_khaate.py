"""add is_khaate to buy_entries and sell_entries

Revision ID: 0007_add_is_khaate
Revises: 0006_shop_profile_address_tax
Create Date: 2026-11-16
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0007_add_is_khaate"
down_revision: Union[str, Sequence[str], None] = "0006_shop_profile_address_tax"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("buy_entries") as batch_op:
        batch_op.add_column(
            sa.Column("is_khaate", sa.Boolean(), server_default=sa.text("false"), nullable=False)
        )
    with op.batch_alter_table("sell_entries") as batch_op:
        batch_op.add_column(
            sa.Column("is_khaate", sa.Boolean(), server_default=sa.text("false"), nullable=False)
        )


def downgrade() -> None:
    with op.batch_alter_table("sell_entries") as batch_op:
        batch_op.drop_column("is_khaate")
    with op.batch_alter_table("buy_entries") as batch_op:
        batch_op.drop_column("is_khaate")
