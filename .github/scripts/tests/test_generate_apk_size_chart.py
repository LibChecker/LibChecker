import importlib.util
import json
import sys
import unittest
from pathlib import Path


sys.dont_write_bytecode = True
SCRIPT_PATH = Path(__file__).resolve().parents[1] / "generate_apk_size_chart.py"
SPEC = importlib.util.spec_from_file_location("generate_apk_size_chart", SCRIPT_PATH)
generate_apk_size_chart = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(generate_apk_size_chart)


def metadata(version, size):
  return json.dumps({
    "app": {
      "version": version,
      "extra": {
        "packageSize": size
      }
    }
  })


class GenerateApkSizeChartTest(unittest.TestCase):
  def test_uses_legacy_app_then_foss_flavor(self):
    legacy = metadata("1.0.0.dev.aaaaaaa", 4 * 1024 * 1024)
    flavored = json.dumps({
      "app": {
        "version": "1.0.0.dev.foss",
        "extra": {
          "packageSize": 2 * 1024 * 1024
        }
      },
      "flavors": {
        "foss": {
          "version": "1.0.0.dev.foss",
          "extra": {
            "packageSize": 3 * 1024 * 1024
          }
        },
        "market": {
          "version": "1.0.0.dev.market",
          "extra": {
            "packageSize": 5 * 1024 * 1024
          }
        }
      }
    })
    records = [
      generate_apk_size_chart.record_from_metadata(
        "a" * 40,
        "2026-01-01",
        legacy
      ),
      generate_apk_size_chart.record_from_metadata(
        "b" * 40,
        "2026-01-02",
        flavored
      ),
      generate_apk_size_chart.record_from_metadata(
        "c" * 40,
        "2026-01-03",
        '{"app":{"extra":{"packageSize": }}}'
      )
    ]

    generate_apk_size_chart.append_pending_record(
      records,
      flavored,
      metadata("1.0.0.dev.ddddddd", 6 * 1024 * 1024),
      "d" * 40,
      "2026-01-04"
    )
    payload = generate_apk_size_chart.build_chart_payload(records)
    dataset = payload["chart"]["data"]["datasets"][0]

    self.assertEqual(
      [point["y"] for point in dataset["data"]],
      [4.0, 3.0, 6.0]
    )
    self.assertEqual(
      [point["x"] for point in dataset["data"]],
      ["2026-01-01", "2026-01-02", "2026-01-04"]
    )
    self.assertEqual(dataset["label"], "FOSS APK")
    self.assertEqual(payload["format"], "svg")
    self.assertEqual(
      payload["chart"]["options"]["plugins"]["subtitle"]["text"],
      "3 measurements · 2026-01-01 – 2026-01-04"
    )
    dark_payload = generate_apk_size_chart.build_chart_payload(records, dark=True)
    self.assertEqual(dark_payload["backgroundColor"], "#0d1117")
    self.assertEqual(
      dark_payload["chart"]["data"]["datasets"][0]["borderColor"],
      "#a78bfa"
    )

  def test_splits_large_history_without_dropping_commits(self):
    records = [
      {
        "commit": f"{index:07x}",
        "date": "2026-01-01",
        "version": "1.0.0",
        "size": (4 * 1024 * 1024) + index
      }
      for index in range(386)
    ]

    datasets = generate_apk_size_chart.build_chart_payload(records)["chart"]["data"]["datasets"]
    points = datasets[0]["data"] + datasets[1]["data"][1:]

    self.assertEqual(len(datasets), 2)
    self.assertLessEqual(max(len(dataset["data"]) for dataset in datasets), 200)
    self.assertEqual(len(points), len(records))

  def test_rejects_invalid_pending_metadata(self):
    with self.assertRaises(ValueError):
      generate_apk_size_chart.append_pending_record(
        [],
        metadata("1.0.0", 4 * 1024 * 1024),
        json.dumps({
          "app": json.loads(metadata("1.0.0", 4 * 1024 * 1024))["app"],
          "flavors": {"foss": {}}
        }),
        "d" * 40,
        "2026-01-04"
      )


if __name__ == "__main__":
  unittest.main()
