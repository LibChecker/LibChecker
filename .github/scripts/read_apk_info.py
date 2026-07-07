import json
import os
import re
from pathlib import Path


PROJECTS_FILE = Path("build-logic/src/main/kotlin/Projects.kt")


def read_sdk(projects_text, name):
  match = re.search(rf"\b{name}\s*=\s*(\d+)", projects_text)
  if not match:
    raise ValueError(f"{name} not found in Projects.kt")
  return match.group(1)


def read_base_version_name(projects_text):
  match = re.search(r'\bconst\s+val\s+baseVersionName\s*=\s*"([^"]+)"', projects_text)
  if not match:
    raise ValueError("baseVersionName not found in Projects.kt")
  return match.group(1)


def read_apk_info(apk_path, projects_file=PROJECTS_FILE):
  apk_path = Path(apk_path)
  if not apk_path.is_file():
    raise FileNotFoundError(apk_path)

  metadata_path = apk_path.parent / "output-metadata.json"
  if not metadata_path.is_file():
    raise FileNotFoundError(f"{metadata_path} not found")

  metadata = json.loads(metadata_path.read_text())
  element = metadata["elements"][0]
  projects_text = Path(projects_file).read_text()
  version_name = element["versionName"]

  return {
    "version-name": version_name,
    "version-code": element["versionCode"],
    "base-version-name": read_base_version_name(projects_text),
    "min-sdk-version": read_sdk(projects_text, "minSdk"),
    "target-sdk-version": read_sdk(projects_text, "targetSdk"),
    "compile-sdk-version": read_sdk(projects_text, "compileSdk"),
    "file-size": apk_path.stat().st_size,
    "is-stable": str(".dev." not in version_name).lower()
  }


def read_update_apk_info(foss_path, market_path=None, projects_file=PROJECTS_FILE):
  info = read_apk_info(foss_path, projects_file)
  if market_path:
    market_info = read_apk_info(market_path, projects_file)
    info.update({
      "market-version-name": market_info["version-name"],
      "market-version-code": market_info["version-code"],
      "market-file-size": market_info["file-size"]
    })
  return info


def write_github_outputs(outputs, output_path):
  with Path(output_path).open("a") as output:
    for key, value in outputs.items():
      output.write(f"{key}={value}\n")


def main():
  write_github_outputs(
    read_update_apk_info(os.environ["FOSS_FILE"], os.environ.get("MARKET_FILE")),
    os.environ["GITHUB_OUTPUT"]
  )


if __name__ == "__main__":
  main()
