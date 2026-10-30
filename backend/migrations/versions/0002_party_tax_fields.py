"""add party tax fields

Revision ID: 0002_party_tax_fields
Revises: 0001_baseline
Create Date: 2026-10-30
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0002_party_tax_fields"
down_revision: Union[str, Sequence[str], None] = "0001_baseline"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("parties") as batch_op:
        batch_op.add_column(sa.Column("gstin", sa.String(length=15), nullable=True))
        batch_op.add_column(sa.Column("mandi_license", sa.String(length=100), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table("parties") as batch_op:
        batch_op.drop_column("mandi_license")
        batch_op.drop_column("gstin")
