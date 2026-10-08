"""Select application checks from the actual PR or merge-queue diff."""

import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess


CI_METADATA = {"release-please-config.json", ".release-please-manifest.json"}


def classify_paths(paths):
    heavy = False
    ci = False
    for path in paths:
        ci_path = path.startswith(".github/") or path in CI_METADATA
        ci = ci or ci_path
        name = PurePosixPath(path)
        prose = (
            (len(name.parts) == 1 and name.suffix.lower() in {".md", ".rst"})
            or path in {"LICENSE", "LICENCE", "LICENSE.txt", "LICENCE.txt", "NOTICE"}
            or (path.startswith("docs/") and name.suffix.lower() in {
                ".md", ".rst", ".txt", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp"
            })
        )
        if not ci_path and not prose:
            heavy = True
    return heavy, ci


def git(*args):
    return subprocess.check_output(["git", *args])


def change_range(event_name, event):
    if event_name == "pull_request":
        source = event["pull_request"]
    elif event_name == "merge_group":
        source = event["merge_group"]
    else:
        return ""
    if event_name == "pull_request":
        base, head = source["base"]["sha"], source["head"]["sha"]
    else:
        base, head = source["base_sha"], source["head_sha"]
    for sha in (base, head):
        if not re.fullmatch(r"[0-9a-f]{40,64}", sha):
            raise ValueError("The event must contain full commit SHAs")
    ancestor = git("merge-base", base, head).decode().strip()
    return f"{ancestor}..{head}"


def classify_event(event_name, event):
    comparison = change_range(event_name, event)
    if not comparison:
        # Scheduled and manual verification deliberately cover the full project.
        return {"heavy": "true", "ci": "true", "range": ""}
    # Count both sides of a rename, including a source file moved into docs.
    paths = git("diff", "--no-renames", "--name-only", "-z", comparison)
    paths = [os.fsdecode(path) for path in paths.split(b"\0") if path]
    git("-c", "core.whitespace=cr-at-eol", "diff", "--check", comparison)
    heavy, ci = classify_paths(paths)
    # Editing a title or body changes no source. Retargeting a PR does.
    if (event_name == "pull_request" and event.get("action") == "edited"
            and "base" not in event.get("changes", {})):
        heavy = ci = False
    return {"heavy": str(heavy).lower(), "ci": str(ci).lower(), "range": comparison}


def main():
    event_name = os.environ["GITHUB_EVENT_NAME"]
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
    outputs = classify_event(event_name, event)
    with open(os.environ["GITHUB_OUTPUT"], "a") as target:
        for key, value in outputs.items():
            target.write(f"{key}={value}\n")
    print(f"Application checks: {outputs['heavy']}; CI checks: {outputs['ci']}")


if __name__ == "__main__":
    main()
