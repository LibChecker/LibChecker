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
  def test_reads_metadata_and_project_sdks(self):
    with tempfile.TemporaryDirectory() as directory:
      root = Path(directory)
      apk = root / "app.apk"
      apk.write_bytes(b"apk")
      (root / "output-metadata.json").write_text(json.dumps({
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
        read_apk_info.read_apk_info(apk, projects),
        {
          "version-name": "2.5.5.dev.test",
          "version-code": 2703,
          "base-version-name": "2.5.5",
          "min-sdk-version": "24",
          "target-sdk-version": "37",
          "compile-sdk-version": "37",
          "file-size": 3,
          "is-stable": "false"
        }
      )


if __name__ == "__main__":
  unittest.main()
