#!/usr/bin/env python3

import html
import json
import os


DEFAULT_LIMIT = 1024
ELLIPSIS = "..."


def build_notification(commit_message, author, commit_url, footer, limit=DEFAULT_LIMIT):
  subject = first_line(commit_message) or "(no commit message)"
  author_html = clamp_html_text(author or "unknown", 120)
  footer_html = clamp_html_text(footer or "", 180)
  url_html = html.escape(commit_url or "", quote=True)

  def render(subject_html):
    lines = [
      "<b>New push to GitHub</b>",
      "",
      f"<b>Commit:</b> {subject_html}",
      f"<b>Author:</b> {author_html}"
    ]
    if url_html:
      lines.append(f'<a href="{url_html}">Commit details</a>')
    if footer_html:
      lines.append(footer_html)
    return "\n".join(lines)

  static_length = len(render(""))
  subject_limit = max(0, limit - static_length)
  subject_html = clamp_html_text(subject, subject_limit)
  text = render(subject_html)

  if len(text) <= limit:
    return text

  # Extremely defensive fallback for unusual URLs/authors/footers.
  compact_lines = [
    "<b>New push to GitHub</b>",
    f'<a href="{url_html}">Commit details</a>' if url_html else "Commit details unavailable"
  ]
  compact = "\n".join(compact_lines)
  return compact if len(compact) <= limit else compact[:limit]


def first_line(value):
  return next((line.strip() for line in value.splitlines() if line.strip()), "")


def clamp_html_text(value, limit):
  if limit <= 0:
    return ""

  escaped = html.escape(value, quote=False)
  if len(escaped) <= limit:
    return escaped

  if limit <= len(ELLIPSIS):
    return ELLIPSIS[:limit]

  suffix = ELLIPSIS
  low = 0
  high = len(value)
  best = suffix
  while low <= high:
    mid = (low + high) // 2
    candidate = html.escape(value[:mid].rstrip(), quote=False) + suffix
    if len(candidate) <= limit:
      best = candidate
      low = mid + 1
    else:
      high = mid - 1
  return best


def write_github_output(values):
  output_path = os.environ.get("GITHUB_OUTPUT")
  if not output_path:
    print(values["text"])
    return

  with open(output_path, "a", encoding="utf-8") as output:
    for key, value in values.items():
      delimiter = unique_delimiter(value)
      if "\n" in value:
        output.write(f"{key}<<{delimiter}\n{value}\n{delimiter}\n")
      else:
        output.write(f"{key}={value}\n")


def unique_delimiter(value):
  delimiter = "TELEGRAM_NOTIFICATION_EOF"
  while delimiter in value:
    delimiter += "_END"
  return delimiter


def main():
  limit = int(os.environ.get("TELEGRAM_TEXT_LIMIT", DEFAULT_LIMIT))
  text = build_notification(
    commit_message=os.environ.get("COMMIT_MESSAGE", ""),
    author=os.environ.get("COMMIT_AUTHOR", ""),
    commit_url=os.environ.get("COMMIT_URL", ""),
    footer=os.environ.get("TELEGRAM_FOOTER", ""),
    limit=limit
  )
  write_github_output({
    "text": text,
    "json": json.dumps(text, ensure_ascii=False)
  })


if __name__ == "__main__":
  main()
