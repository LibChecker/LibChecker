#!/usr/bin/env python3

import argparse
import json
import os


def build_media_group(caption_json, media_name="marketRelease"):
  return [{
    "type": "document",
    "media": f"attach://{media_name}",
    "parse_mode": "HTML",
    "caption": json.loads(caption_json)
  }]


def build_message(channel_id, message_json):
  return {
    "chat_id": channel_id,
    "parse_mode": "HTML",
    "text": json.loads(message_json)
  }


def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument("--mode", choices=("media-group", "message"), required=True)
  return parser.parse_args()


def main():
  args = parse_args()
  if args.mode == "media-group":
    payload = build_media_group(os.environ["TELEGRAM_CAPTION_JSON"])
  else:
    payload = build_message(
      os.environ["CHANNEL_ID"],
      os.environ["TELEGRAM_MESSAGE_JSON"]
    )
  print(json.dumps(payload, ensure_ascii=False))


if __name__ == "__main__":
  main()
