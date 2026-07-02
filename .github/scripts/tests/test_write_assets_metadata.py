import argparse
import importlib.util
import sys
import unittest
from pathlib import Path


sys.dont_write_bytecode = True
SCRIPT_PATH = Path(__file__).resolve().parents[1] / "write_assets_metadata.py"
SPEC = importlib.util.spec_from_file_location("write_assets_metadata", SCRIPT_PATH)
write_assets_metadata = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(write_assets_metadata)


class WriteAssetsMetadataTest(unittest.TestCase):
  def test_builds_stable_metadata(self):
    args = argparse.Namespace(
      version="2.5.5",
      version_code=2703,
      target=37,
      min=24,
      compile=37,
      package_size=4339135,
      link="https://github.com/LibChecker/LibChecker/releases/download/2.5.5/app.apk",
      note="https://github.com/LibChecker/LibChecker/releases"
    )

    self.assertEqual(
      write_assets_metadata.build_metadata(args),
      {
        "app": {
          "version": "2.5.5",
          "versionCode": 2703,
          "extra": {
            "target": 37,
            "min": 24,
            "compile": 37,
            "packageSize": 4339135
          },
          "link": "https://github.com/LibChecker/LibChecker/releases/download/2.5.5/app.apk",
          "note": "https://github.com/LibChecker/LibChecker/releases"
        }
      }
    )


if __name__ == "__main__":
  unittest.main()
