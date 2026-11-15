"""add address, gstin, licence_no to shop_profiles

Revision ID: 0006_shop_profile_address_tax
Revises: 0005_loan_interest_rate
Create Date: 2026-11-15
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0006_shop_profile_address_tax"
down_revision: Union[str, Sequence[str], None] = "0005_loan_interest_rate"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("shop_profiles") as batch_op:
        batch_op.add_column(sa.Column("address", sa.String(), nullable=True))
        batch_op.add_column(sa.Column("gstin", sa.String(15), nullable=True))
        batch_op.add_column(sa.Column("licence_no", sa.String(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table("shop_profiles") as batch_op:
        batch_op.drop_column("licence_no")
        batch_op.drop_column("gstin")
        batch_op.drop_column("address")
