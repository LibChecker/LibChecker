#!/usr/bin/env python3
"""Create a recent .lcss fixture for TimeNodePickerCompatibilityInstrumentedTest.

Push the output to /data/local/tmp/ on a test device and pass its path as the
snapshotFixturePath instrumentation argument. Import leaves three snapshots
containing one added, one removed, one updated and one unchanged app.
"""

import argparse
from datetime import datetime, timedelta, timezone
from pathlib import Path


def varint(value):
    encoded = bytearray()
    while value > 127:
        encoded.append((value & 127) | 128)
        value >>= 7
    encoded.append(value)
    return encoded


def field(number, value):
    if isinstance(value, str):
        data = value.encode("utf-8")
        return varint(number * 8 + 2) + varint(len(data)) + data
    return varint(number * 8) + varint(value)


def create_fixture():
    today = datetime.now(timezone.utc).replace(hour=12, minute=0, second=0, microsecond=0)
    timestamps = [int((today - timedelta(days=days)).timestamp() * 1000) for days in (7, 3, 1)]
    archive = bytearray()
    for index, timestamp in enumerate(timestamps):
        for name in ("reader", "notes", "legacy" if index < 2 else "added"):
            version = index + 1 if name == "reader" else 1
            updated = timestamp - 3_600_000 if name in ("reader", "added") else timestamps[0] - 7_200_000
            # Field numbers follow app/src/main/proto/LibChecker.proto.
            fields = {
                1: f"com.example.lcfixture.{name}",
                2: timestamp,
                3: f"Fixture {name.title()}",
                4: f"{version}.0",
                5: version,
                6: timestamps[0] - 5 * 86_400_000,
                7: updated,
                8: 0,
                9: 3,
                10: 25,
                18: 1_048_576 * version,
                19: 25,
                20: 24,
                22: "[]",
                24: "[]",
            }
            fields.update({number: "[]" for number in range(11, 18)})
            if name == "reader" and index > 0:
                fields[16] = '["android.permission.INTERNET"]'
            message = b"".join(field(number, value) for number, value in sorted(fields.items()))
            archive += varint(len(message)) + message
    return archive


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    with args.output.open("xb") as output:
        output.write(create_fixture())
    print(f"Created {args.output}: 3 timestamps, 9 records")
