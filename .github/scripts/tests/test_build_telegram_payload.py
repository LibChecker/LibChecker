import importlib.util
import json
from pathlib import Path
import sys
import unittest


sys.dont_write_bytecode = True
SCRIPT_PATH = Path(__file__).resolve().parents[1] / "build_telegram_payload.py"
SPEC = importlib.util.spec_from_file_location("build_telegram_payload", SCRIPT_PATH)
build_telegram_payload = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(build_telegram_payload)


class BuildTelegramPayloadTest(unittest.TestCase):
  def test_builds_media_group_payload(self):
    self.assertEqual(
      build_telegram_payload.build_media_group(json.dumps("caption")),
      [{
        "type": "document",
        "media": "attach://marketRelease",
        "parse_mode": "HTML",
        "caption": "caption"
      }]
    )

  def test_builds_message_payload(self):
    self.assertEqual(
      build_telegram_payload.build_message("channel", json.dumps("message")),
      {
        "chat_id": "channel",
        "parse_mode": "HTML",
        "text": "message"
      }
    )


if __name__ == "__main__":
  unittest.main()
