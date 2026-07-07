import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


sys.dont_write_bytecode = True
SCRIPT_PATH = Path(__file__).resolve().parents[1] / "read_apk_info.py"
SPEC = importlib.util.spec_from_file_location("read_apk_info", SCRIPT_PATH)
read_apk_info = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(read_apk_info)


class ReadApkInfoTest(unittest.TestCase):
  def test_reads_foss_and_market_metadata(self):
    with tempfile.TemporaryDirectory() as directory:
      root = Path(directory)
      foss_dir = root / "foss"
      market_dir = root / "market"
      foss_dir.mkdir()
      market_dir.mkdir()
      foss_apk = foss_dir / "app.apk"
      market_apk = market_dir / "app.apk"
      foss_apk.write_bytes(b"apk")
      market_apk.write_bytes(b"market")
      (foss_dir / "output-metadata.json").write_text(json.dumps({
        "elements": [{
          "versionName": "2.5.5.dev.test",
          "versionCode": 2703
        }]
      }))
      (market_dir / "output-metadata.json").write_text(json.dumps({
        "elements": [{
          "versionName": "2.5.5.dev.test",
          "versionCode": 2703
        }]
      }))
      projects = root / "Projects.kt"
      projects.write_text("""
        const val baseVersionName = "2.5.5"
        minSdk = 24
        targetSdk = 37
        compileSdk = 37
      """)

      self.assertEqual(
        read_apk_info.read_update_apk_info(foss_apk, market_apk, projects),
        {
          "version-name": "2.5.5.dev.test",
          "version-code": 2703,
          "base-version-name": "2.5.5",
          "min-sdk-version": "24",
          "target-sdk-version": "37",
          "compile-sdk-version": "37",
          "file-size": 3,
          "market-version-name": "2.5.5.dev.test",
          "market-version-code": 2703,
          "market-file-size": 6,
          "is-stable": "false"
        }
      )


if __name__ == "__main__":
  unittest.main()
