from pathlib import Path

from app.schemas.events import EventsBatchRequest

# The same sample file is checked against the client by AnalyticsWireContractTest in core-data.
SAMPLE = Path(__file__).resolve().parents[2] / "core-data/src/jvmTest/resources/telemetry-events.sample.json"


def test_client_sample_is_accepted_by_the_server_schema():
    batch = EventsBatchRequest.model_validate_json(SAMPLE.read_text())

    assert [event.name for event in batch.events] == ["screen_view", "sync_ok"]
