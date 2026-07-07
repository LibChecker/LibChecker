import argparse
import json
from pathlib import Path


def build_app_metadata(version, version_code, target, min_sdk, compile_sdk, package_size, link, note=None):
  app = {
    "version": version,
    "versionCode": version_code,
    "extra": {
      "target": target,
      "min": min_sdk,
      "compile": compile_sdk,
      "packageSize": package_size
    },
    "link": link
  }
  if note:
    app["note"] = note
  return app


def build_metadata(args):
  foss_app = build_app_metadata(
    args.version,
    args.version_code,
    args.target,
    args.min,
    args.compile,
    args.package_size,
    args.link,
    args.note
  )
  metadata = {
    "app": foss_app,
    "flavors": {
      "foss": foss_app
    }
  }

  market_link = getattr(args, "market_link", None)
  if market_link:
    metadata["flavors"]["market"] = build_app_metadata(
      getattr(args, "market_version", None) or args.version,
      getattr(args, "market_version_code", None) or args.version_code,
      args.target,
      args.min,
      args.compile,
      getattr(args, "market_package_size", None) or args.package_size,
      market_link,
      args.note
    )
  return metadata


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
  parser.add_argument("--market-version")
  parser.add_argument("--market-version-code", type=int)
  parser.add_argument("--market-package-size", type=int)
  parser.add_argument("--market-link")
  return parser.parse_args()


def main():
  args = parse_args()
  Path(args.output).write_text(json.dumps(build_metadata(args), indent=2) + "\n")


if __name__ == "__main__":
  main()
