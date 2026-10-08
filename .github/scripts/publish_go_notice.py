"""Keep Java repository and release entry points directed to their Go replacement."""

import json
import os
from pathlib import Path
import urllib.error
import urllib.request


START = "<!-- opendiscogs-go-migration:start -->"
END = "<!-- opendiscogs-go-migration:end -->"


def release_body(body, config):
    body = body or ""
    notice = (
        f"{START}\n"
        f"This Java implementation is deprecated. Use "
        f"[{config['replacement_name']}](https://github.com/{config['replacement']}).\n\n"
        f"See the [migration guide](https://github.com/{config['repository']}"
        "/blob/main/docs/migration-to-go.md) before switching an existing deployment.\n"
        f"{END}\n\n"
    )
    if body.startswith(START):
        end = body.find(END)
        if end < 0:
            raise ValueError("Incomplete migration notice; refusing to overwrite release notes")
        body = body[end + len(END):]
        if body.startswith("\n\n"):
            body = body[2:]
    return notice + body


def request(method, path, token, payload=None):
    req = urllib.request.Request(
        "https://api.github.com/" + path,
        data=None if payload is None else json.dumps(payload).encode(),
        method=method,
        headers={
            "Authorization": "Bearer " + token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
            "User-Agent": "opendiscogs-go-migration",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"GitHub {method} {path} failed: HTTP {error.code}") from None


def main():
    config = json.loads((Path(__file__).parents[1] / "go-migration.json").read_text())
    repo = os.environ["GITHUB_REPOSITORY"]
    if repo != config["repository"]:
        raise ValueError("Migration configuration does not match this repository")
    token = os.environ["GH_TOKEN"]
    current = request("GET", f"repos/{repo}", token)
    metadata = {
        "description": config["description"],
        "homepage": "https://github.com/" + config["replacement"],
    }
    if any(current.get(key) != value for key, value in metadata.items()):
        request("PATCH", f"repos/{repo}", token, metadata)
        print("Updated repository description and website")

    updated = 0
    page = 1
    while True:
        releases = request("GET", f"repos/{repo}/releases?per_page=100&page={page}", token)
        for release in releases:
            if release["draft"]:
                continue
            body = release_body(release["body"], config)
            if body != (release["body"] or ""):
                request("PATCH", f"repos/{repo}/releases/{release['id']}", token, {"body": body})
                updated += 1
                print(f"Updated migration notice: {release['tag_name']}")
        if len(releases) < 100:
            break
        page += 1
    print(f"Updated {updated} release notices")


if __name__ == "__main__":
    main()
