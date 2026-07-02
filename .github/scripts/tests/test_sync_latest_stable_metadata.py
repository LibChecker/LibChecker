import argparse
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


sys.dont_write_bytecode = True
SCRIPT_PATH = Path(__file__).resolve().parents[1] / "sync_latest_stable_metadata.py"
SPEC = importlib.util.spec_from_file_location("sync_latest_stable_metadata", SCRIPT_PATH)
sync_latest_stable_metadata = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sync_latest_stable_metadata)


class SyncLatestStableMetadataTest(unittest.TestCase):
  def test_updates_outdated_stable_metadata(self):
    release = {
      "tag_name": "2.5.4",
      "assets": [{
        "name": "LibChecker-2.5.4.5696014-2671-foss-release.apk",
        "size": 4779938,
        "browser_download_url": "https://github.com/LibChecker/LibChecker/releases/download/2.5.4/LibChecker-2.5.4.5696014-2671-foss-release.apk"
      }]
    }
    args = argparse.Namespace(target=37, min=24, compile=37)

    with tempfile.TemporaryDirectory() as directory:
      stable = Path(directory) / "stable.json"
      stable.write_text(json.dumps({"app": {"version": "2.5.3"}}))

      self.assertTrue(sync_latest_stable_metadata.sync_latest_stable(stable, args, release))
      self.assertEqual(
        json.loads(stable.read_text()),
        {
          "app": {
            "version": "2.5.4",
            "versionCode": 2671,
            "extra": {
              "target": 37,
              "min": 24,
              "compile": 37,
              "packageSize": 4779938
            },
            "link": "https://github.com/LibChecker/LibChecker/releases/download/2.5.4/LibChecker-2.5.4.5696014-2671-foss-release.apk",
            "note": "https://github.com/LibChecker/LibChecker/releases"
          }
        }
      )

  def test_skips_current_stable_metadata(self):
    release = {"tag_name": "2.5.4", "assets": []}
    args = argparse.Namespace(target=37, min=24, compile=37)

    with tempfile.TemporaryDirectory() as directory:
      stable = Path(directory) / "stable.json"
      stable.write_text(json.dumps({"app": {"version": "2.5.4"}}))

      self.assertFalse(sync_latest_stable_metadata.sync_latest_stable(stable, args, release))


if __name__ == "__main__":
  unittest.main()
