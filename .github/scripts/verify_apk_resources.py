"""Verify that APK size optimizations preserve resources and bundled app data."""

import argparse
import re
import subprocess
import zipfile


def resource_dump(aapt2, apk):
  result = subprocess.check_output([aapt2, "dump", "resources", apk], text=True)
  # Crashlytics generates a new mapping ID on each release build.
  return re.sub(
    r'(string/com\.google\.firebase\.crashlytics\.mapping_file_id\n\s+\(\) )"[^"\n]*"',
    r'\1"<build-specific>"',
    result,
  )


def without_redundant_rasters(dump, min_sdk):
  removed, lines = set(), []
  for block in re.split(r'(?=    resource )', dump):
    vector = re.search(r'^\s+\(anydpi(?:-v(\d+))?\) \(file\) res/\S+\.xml type=XML$', block, re.M)
    covered = vector and int(vector[1] or 21) <= min_sdk
    for line in block.splitlines():
      raster = re.fullmatch(
        r'\s+\((?:ldpi|mdpi|tvdpi|hdpi|xhdpi|xxhdpi|xxxhdpi)(?:-v(\d+))?\) '
        r'\(file\) (res/\S+\.png) type=PNG', line,
      )
      if covered and raster and int(raster[1] or 1) <= min_sdk:
        removed.add(raster[2])
      else:
        lines.append(line)
  return lines, removed


def verify(before, after, aapt2, ffmpeg=None, allow_redundant_rasters=False):
  def pixels(data):
    # PAM includes dimensions as well as every RGBA byte, including transparent pixels.
    return subprocess.check_output(
      [ffmpeg, "-v", "error", "-i", "pipe:0", "-f", "image2pipe",
       "-vcodec", "pam", "-pix_fmt", "rgba", "-"], input=data,
    )

  with zipfile.ZipFile(before) as old, zipfile.ZipFile(after) as new:
    old_dump, new_dump = resource_dump(aapt2, before), resource_dump(aapt2, after)
    old_lines, new_lines = old_dump.splitlines(), new_dump.splitlines()
    redundant = set()
    if allow_redundant_rasters:
      badging = subprocess.check_output([aapt2, "dump", "badging", after], text=True)
      min_sdk = int(re.search(r"(?:minSdkVersion|sdkVersion):'(\d+)'", badging)[1])
      old_lines, redundant = without_redundant_rasters(old_dump, min_sdk)
      new_lines, _ = without_redundant_rasters(new_dump, min_sdk)
    if len(old_lines) != len(new_lines):
      raise AssertionError("Resource IDs, configurations, names, or values changed")
    converted = {}
    for old_line, new_line in zip(old_lines, new_lines):
      if old_line == new_line:
        continue
      old_image = re.fullmatch(r'(\s+\([^\n]*\) \(file\) )(res/\S+\.png) type=PNG', old_line)
      new_image = re.fullmatch(r'(\s+\([^\n]*\) \(file\) )(res/\S+\.webp)', new_line)
      if not (ffmpeg and old_image and new_image and old_image[1] == new_image[1]):
        raise AssertionError(f"Resource changed:\n{old_line}\n{new_line}")

      old_name, new_name = old_image[2], new_image[2]
      if pixels(old.read(old_name)) != pixels(new.read(new_name)):
        raise AssertionError(f"Image pixels changed: {old_name} -> {new_name}")
      converted[new_name] = old_name

    def contents(archive):
      return {
        name: archive.read(name)
        for name in archive.namelist()
        if (name.startswith(("res/", "assets/")) or name == "AndroidManifest.xml")
        and not name.startswith("assets/dexopt/")
      }

    old_files, new_files = contents(old), contents(new)
    removed_rasters = redundant - new_files.keys()
    for name in removed_rasters:
      del old_files[name]
    for new_name, old_name in converted.items():
      del new_files[new_name]
      new_files[old_name] = old_files[old_name]
    changed = []
    for name in sorted(old_files.keys() | new_files.keys()):
      old_data, new_data = old_files.get(name), new_files.get(name)
      if old_data == new_data:
        continue
      if ffmpeg and name.endswith(".webp") and old_data and new_data:
        if pixels(old_data) == pixels(new_data):
          converted[name] = name
          continue
      changed.append(name)
    if changed:
      raise AssertionError(f"Resource or bundled asset contents changed: {changed}")
  print(f"Equivalent resources and bundled data; {len(converted)} lossless images, "
        f"{len(removed_rasters)} unreachable raster files removed.")


if __name__ == "__main__":
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("before")
  parser.add_argument("after")
  parser.add_argument("--aapt2", required=True)
  parser.add_argument("--ffmpeg", help="Allow WebP optimization only when decoded RGBA is identical")
  parser.add_argument("--allow-redundant-rasters", action="store_true",
                      help="Allow raster removal only when anydpi covers all supported Android versions")
  args = parser.parse_args()
  verify(args.before, args.after, args.aapt2, args.ffmpeg, args.allow_redundant_rasters)
