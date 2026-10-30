import os
import tempfile
from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect

from app.database import Base
import app.models  # noqa: F401 - ensure all models are registered in Base.metadata


def test_alembic_upgrade_and_downgrade():
    """Verify that Alembic migrations upgrade an empty database to match Base.metadata

    exactly, and that downgrade back to base drops all tables.
    """
    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        db_path = tmp.name

    try:
        url = f"sqlite+aiosqlite:///{db_path}"
        # Locate alembic.ini relative to this test file
        backend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        ini_path = os.path.join(backend_dir, "alembic.ini")
        cfg = Config(ini_path)
        cfg.set_main_option("script_location", os.path.join(backend_dir, "migrations"))
        cfg.set_main_option("sqlalchemy.url", url)

        # 1. Upgrade from empty to head
        command.upgrade(cfg, "head")

        # 2. Inspect created tables and columns
        sync_engine = create_engine(f"sqlite:///{db_path}")
        try:
            inspector = inspect(sync_engine)
            actual_tables = set(inspector.get_table_names())
            actual_tables.discard("alembic_version")

            expected_tables = set(Base.metadata.tables.keys())
            assert actual_tables == expected_tables, (
                f"Tables mismatch.\nExpected: {expected_tables}\nActual: {actual_tables}"
            )

            for table_name in expected_tables:
                actual_cols = {col["name"] for col in inspector.get_columns(table_name)}
                expected_cols = {col.name for col in Base.metadata.tables[table_name].columns}
                assert actual_cols == expected_cols, (
                    f"Columns mismatch in table '{table_name}'.\n"
                    f"Missing in DB: {expected_cols - actual_cols}\n"
                    f"Unexpected in DB: {actual_cols - expected_cols}"
                )

            # Explicitly verify new pilot columns are present in DB
            settlement_cols = {col["name"] for col in inspector.get_columns("lend_settlements")}
            assert "principal_paisa" in settlement_cols
            assert "interest_paisa" in settlement_cols
            assert "calculated_interest_paisa" in settlement_cols

            buy_cols = {col["name"] for col in inspector.get_columns("buy_entries")}
            assert "slip_no" in buy_cols

            sell_cols = {col["name"] for col in inspector.get_columns("sell_entries")}
            assert "slip_no" in sell_cols

            lend_cols = {col["name"] for col in inspector.get_columns("lend_entries")}
            assert "slip_no" in lend_cols

            party_cols = {col["name"] for col in inspector.get_columns("parties")}
            assert {"gstin", "mandi_license"} <= party_cols
        finally:
            sync_engine.dispose()

        # 3. Downgrade back to base
        command.downgrade(cfg, "base")

        sync_engine_post = create_engine(f"sqlite:///{db_path}")
        try:
            inspector_post = inspect(sync_engine_post)
            post_downgrade_tables = set(inspector_post.get_table_names())
            post_downgrade_tables.discard("alembic_version")
            assert len(post_downgrade_tables) == 0, (
                f"Tables remained after downgrade to base: {post_downgrade_tables}"
            )
        finally:
            sync_engine_post.dispose()
    finally:
        if os.path.exists(db_path):
            os.remove(db_path)
