import argparse
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen


MEBIBYTE = 1024 * 1024


def parse_metadata(text):
  try:
    metadata = json.loads(text)
    flavors = metadata.get("flavors")
    app = flavors["foss"] if flavors is not None else metadata["app"]
    version = str(app["version"])
    size = app["extra"]["packageSize"]
  except (json.JSONDecodeError, KeyError, TypeError) as error:
    raise ValueError("ci.json is missing valid FOSS app metadata") from error
  if isinstance(size, bool) or not isinstance(size, int) or size <= 0:
    raise ValueError("ci.json packageSize must be a positive integer")
  return version, size


def record_from_metadata(commit, date, text, strict=False):
  try:
    version, size = parse_metadata(text)
  except ValueError:
    if strict:
      raise
    version, size = "Unavailable", None
  return {
    "commit": commit[:7],
    "date": date,
    "version": version,
    "size": size
  }


def append_pending_record(records, head_text, working_text, commit, date):
  if working_text != head_text:
    records.append(record_from_metadata(commit, date, working_text, strict=True))
  return records


def run_git(repository, *args):
  return subprocess.run(
    ["git", "-C", str(repository), *args],
    check=True,
    capture_output=True,
    text=True,
    encoding="utf-8"
  ).stdout


def load_records(repository, pending_commit=None, pending_date=None):
  records = []
  history = run_git(
    repository,
    "log",
    "--reverse",
    "--format=%H%x09%cs",
    "--",
    "ci.json"
  )
  # ponytail: one git show per metadata commit; use cat-file --batch if history grows into thousands.
  for line in history.splitlines():
    commit, date = line.split("\t", 1)
    text = run_git(repository, "show", f"{commit}:ci.json")
    records.append(record_from_metadata(commit, date, text))

  head_text = run_git(repository, "show", "HEAD:ci.json")
  working_text = (repository / "ci.json").read_text(encoding="utf-8")
  append_pending_record(
    records,
    head_text,
    working_text,
    pending_commit or "pending",
    pending_date or datetime.now(timezone.utc).date().isoformat()
  )
  if not any(record["size"] is not None for record in records):
    raise ValueError("ci.json history has no valid package sizes")
  return records


def build_chart_payload(records, dark=False):
  records = [record for record in records if record["size"] is not None]
  date_range = f"{records[0]['date']} – {records[-1]['date']}"
  points = [
    {
      "x": record["date"],
      "y": round(record["size"] / MEBIBYTE, 3)
    }
    for record in records
  ]
  background_color = "#0d1117" if dark else "#ffffff"
  title_color = "#f0f6fc" if dark else "#111827"
  text_color = "#8b949e" if dark else "#6b7280"
  line_color = "#a78bfa" if dark else "#7c3aed"
  fill_color = "rgba(167, 139, 250, 0.16)" if dark else "rgba(124, 58, 237, 0.12)"
  grid_color = "rgba(139, 148, 158, 0.22)" if dark else "rgba(148, 163, 184, 0.25)"

  base_dataset = {
    "label": "FOSS APK",
    "borderColor": line_color,
    "backgroundColor": fill_color,
    "borderWidth": 2.5,
    "pointRadius": 0,
    "pointHoverRadius": 4,
    "tension": 0.22,
    "fill": True,
    "spanGaps": False
  }
  datasets = []
  for start in range(0, len(points), 200):
    chunk = points[max(0, start - 1):start + 200]
    dataset = dict(base_dataset)
    dataset["data"] = chunk
    datasets.append(dataset)

  return {
    "version": "4",
    "width": 1200,
    "height": 480,
    "format": "svg",
    "backgroundColor": background_color,
    "chart": {
      "type": "line",
      "data": {
        "datasets": datasets
      },
      "options": {
        "responsive": False,
        "animation": False,
        "layout": {
          "padding": {
            "top": 24,
            "right": 28,
            "bottom": 12,
            "left": 18
          }
        },
        "plugins": {
          "title": {
            "display": True,
            "text": "FOSS APK size history",
            "align": "start",
            "color": title_color,
            "font": {
              "size": 22,
              "weight": "600"
            },
            "padding": {
              "bottom": 4
            }
          },
          "subtitle": {
            "display": True,
            "text": f"{len(records)} measurements · {date_range}",
            "align": "start",
            "color": text_color,
            "font": {
              "size": 12
            },
            "padding": {
              "bottom": 20
            }
          },
          "legend": {
            "display": False
          },
          "tooltip": {
            "enabled": False
          }
        },
        "scales": {
          "x": {
            "grid": {
              "display": False
            },
            "border": {
              "display": False
            },
            "ticks": {
              "autoSkip": True,
              "maxTicksLimit": 8,
              "maxRotation": 0,
              "color": text_color,
              "font": {
                "size": 11
              }
            }
          },
          "y": {
            "grid": {
              "color": grid_color,
              "drawTicks": False
            },
            "border": {
              "display": False
            },
            "title": {
              "display": True,
              "text": "MiB",
              "color": text_color
            },
            "ticks": {
              "color": text_color,
              "padding": 10
            }
          }
        }
      }
    }
  }


def render_chart(payload, endpoint="https://quickchart.io/chart"):
  request = Request(
    endpoint,
    data=json.dumps(payload).encode("utf-8"),
    headers={
      "Content-Type": "application/json",
      "User-Agent": "LibChecker-assets-chart/1.0"
    },
    method="POST"
  )
  with urlopen(request, timeout=30) as response:
    svg = response.read()
  if b"<svg" not in svg[:500]:
    raise ValueError("QuickChart did not return an SVG")
  return svg


def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument("--repository", default=".")
  parser.add_argument("--output", required=True)
  parser.add_argument("--dark-output")
  parser.add_argument("--pending-commit")
  parser.add_argument("--pending-date")
  parser.add_argument("--endpoint", default="https://quickchart.io/chart")
  return parser.parse_args()


def main():
  args = parse_args()
  repository = Path(args.repository).resolve()
  records = load_records(repository, args.pending_commit, args.pending_date)
  light_svg = render_chart(build_chart_payload(records), args.endpoint)
  dark_svg = render_chart(build_chart_payload(records, dark=True), args.endpoint) if args.dark_output else None
  Path(args.output).write_bytes(light_svg)
  if dark_svg is not None:
    Path(args.dark_output).write_bytes(dark_svg)


if __name__ == "__main__":
  main()
