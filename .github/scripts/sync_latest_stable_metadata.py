#!/usr/bin/env python3

import argparse
import json
import os
import re
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from write_assets_metadata import build_metadata


RELEASES_URL = "https://api.github.com/repos/LibChecker/LibChecker/releases/latest"
RELEASES_NOTE_URL = "https://github.com/LibChecker/LibChecker/releases"


def load_json_url(url):
  request = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json"})
  token = os.environ.get("GITHUB_TOKEN")
  if token:
    request.add_header("Authorization", f"Bearer {token}")
  with urllib.request.urlopen(request, timeout=30) as response:
    return json.loads(response.read().decode())


def current_stable_version(path):
  path = Path(path)
  if not path.is_file():
    return None
  return json.loads(path.read_text()).get("app", {}).get("version")


def find_apk_asset(release, flavor, required=True):
  suffix = f"-{flavor}-release.apk"
  for asset in release.get("assets", []):
    name = asset.get("name", "")
    if name.startswith("LibChecker-") and name.endswith(suffix):
      return asset
  if required:
    raise ValueError(f"{flavor} release APK asset not found")
  return None


def version_code_from_asset_name(name, flavor):
  match = re.search(rf"-(\d+)-{flavor}-release\.apk$", name)
  if not match:
    raise ValueError(f"versionCode not found in {name}")
  return int(match.group(1))


def latest_stable_metadata(release, args):
  foss_asset = find_apk_asset(release, "foss")
  market_asset = find_apk_asset(release, "market", required=False)
  metadata_args = argparse.Namespace(
    version=release["tag_name"],
    version_code=version_code_from_asset_name(foss_asset["name"], "foss"),
    target=args.target,
    min=args.min,
    compile=args.compile,
    package_size=foss_asset["size"],
    link=foss_asset["browser_download_url"],
    note=RELEASES_NOTE_URL,
    market_version=None,
    market_version_code=None,
    market_package_size=None,
    market_link=None
  )
  if market_asset:
    metadata_args.market_version = release["tag_name"]
    metadata_args.market_version_code = version_code_from_asset_name(market_asset["name"], "market")
    metadata_args.market_package_size = market_asset["size"]
    metadata_args.market_link = market_asset["browser_download_url"]
  return build_metadata(metadata_args)


def sync_latest_stable(output, args, release=None):
  release = release or load_json_url(RELEASES_URL)
  if current_stable_version(output) == release["tag_name"]:
    return False

  Path(output).write_text(json.dumps(latest_stable_metadata(release, args), indent=2) + "\n")
  return True


def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument("--output", required=True)
  parser.add_argument("--target", required=True, type=int)
  parser.add_argument("--min", required=True, type=int)
  parser.add_argument("--compile", required=True, type=int)
  return parser.parse_args()


def main():
  args = parse_args()
  updated = sync_latest_stable(args.output, args)
  print("stable.json updated" if updated else "stable.json already latest")


if __name__ == "__main__":
  main()
