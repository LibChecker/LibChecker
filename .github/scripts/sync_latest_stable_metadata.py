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


def find_foss_apk_asset(release):
  for asset in release.get("assets", []):
    name = asset.get("name", "")
    if name.startswith("LibChecker-") and name.endswith("-foss-release.apk"):
      return asset
  raise ValueError("FOSS release APK asset not found")


def version_code_from_asset_name(name):
  match = re.search(r"-(\d+)-foss-release\.apk$", name)
  if not match:
    raise ValueError(f"versionCode not found in {name}")
  return int(match.group(1))


def latest_stable_metadata(release, args):
  asset = find_foss_apk_asset(release)
  return build_metadata(argparse.Namespace(
    version=release["tag_name"],
    version_code=version_code_from_asset_name(asset["name"]),
    target=args.target,
    min=args.min,
    compile=args.compile,
    package_size=asset["size"],
    link=asset["browser_download_url"],
    note=RELEASES_NOTE_URL
  ))


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
