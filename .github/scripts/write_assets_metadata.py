import argparse
import json
from pathlib import Path


def build_metadata(args):
  app = {
    "version": args.version,
    "versionCode": args.version_code,
    "extra": {
      "target": args.target,
      "min": args.min,
      "compile": args.compile,
      "packageSize": args.package_size
    },
    "link": args.link
  }
  if args.note:
    app["note"] = args.note
  return {"app": app}


def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument("--output", required=True)
  parser.add_argument("--version", required=True)
  parser.add_argument("--version-code", required=True, type=int)
  parser.add_argument("--target", required=True, type=int)
  parser.add_argument("--min", required=True, type=int)
  parser.add_argument("--compile", required=True, type=int)
  parser.add_argument("--package-size", required=True, type=int)
  parser.add_argument("--link", required=True)
  parser.add_argument("--note")
  return parser.parse_args()


def main():
  args = parse_args()
  Path(args.output).write_text(json.dumps(build_metadata(args), indent=2) + "\n")


if __name__ == "__main__":
  main()
