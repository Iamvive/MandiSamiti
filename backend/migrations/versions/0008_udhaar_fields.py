"""udhaar direction, kind, receiver on lend entries; lend link on cash transactions

Revision ID: 0008_udhaar_fields
Revises: 0007_add_is_khaate
Create Date: 2026-11-18
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "0008_udhaar_fields"
down_revision: Union[str, Sequence[str], None] = "0007_add_is_khaate"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.add_column(sa.Column("direction", sa.String(10), nullable=False, server_default="GIVEN"))
        batch_op.add_column(sa.Column("kind", sa.String(10), nullable=False, server_default="LOAN"))
        batch_op.add_column(sa.Column("receiver_name", sa.String(), nullable=True))
        batch_op.add_column(sa.Column("receiver_phone", sa.String(), nullable=True))
    with op.batch_alter_table("cash_transactions") as batch_op:
        batch_op.add_column(sa.Column("lend_id", sa.String(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table("cash_transactions") as batch_op:
        batch_op.drop_column("lend_id")
    with op.batch_alter_table("lend_entries") as batch_op:
        batch_op.drop_column("receiver_phone")
        batch_op.drop_column("receiver_name")
        batch_op.drop_column("kind")
        batch_op.drop_column("direction")
