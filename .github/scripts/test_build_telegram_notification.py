import importlib.util
from pathlib import Path
import unittest


SCRIPT_PATH = Path(__file__).with_name("build_telegram_notification.py")


def load_module():
  spec = importlib.util.spec_from_file_location("build_telegram_notification", SCRIPT_PATH)
  module = importlib.util.module_from_spec(spec)
  spec.loader.exec_module(module)
  return module


class BuildTelegramNotificationTest(unittest.TestCase):
  def setUp(self):
    self.module = load_module()

  def test_long_commit_message_is_summarized_under_caption_limit(self):
    text = self.module.build_notification(
      commit_message="Fix release notes\n\n" + "x" * 5000,
      author="Absinthe",
      commit_url="https://github.com/LibChecker/LibChecker/commit/abcdef",
      footer="Snapshot apk is attached",
      limit=1024
    )

    self.assertLessEqual(len(text), 1024)
    self.assertIn("Fix release notes", text)
    self.assertNotIn("x" * 200, text)
    self.assertIn('<a href="https://github.com/LibChecker/LibChecker/commit/abcdef">Commit details</a>', text)
    self.assertIn("Snapshot apk is attached", text)

  def test_long_commit_subject_is_truncated_under_caption_limit(self):
    text = self.module.build_notification(
      commit_message="T" * 5000,
      author="Absinthe",
      commit_url="https://github.com/LibChecker/LibChecker/commit/abcdef",
      footer="Snapshot apk is attached",
      limit=1024
    )

    self.assertLessEqual(len(text), 1024)
    self.assertIn("TTT", text)
    self.assertIn("...", text)
    self.assertNotIn("T" * 1000, text)

  def test_escapes_html_from_commit_and_author(self):
    text = self.module.build_notification(
      commit_message="Fix <danger> & regression",
      author="A&B <bot>",
      commit_url="https://github.com/LibChecker/LibChecker/commit/abcdef",
      footer="This push skipped building",
      limit=4096
    )

    self.assertIn("Fix &lt;danger&gt; &amp; regression", text)
    self.assertIn("A&amp;B &lt;bot&gt;", text)
    self.assertIn("This push skipped building", text)


if __name__ == "__main__":
  unittest.main()
