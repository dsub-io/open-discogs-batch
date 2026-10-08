#!/usr/bin/env bash
set -euo pipefail

tool_dir="$(mktemp -d)"
trap 'rm -rf "$tool_dir"' EXIT
archive=actionlint_1.7.12_linux_amd64.tar.gz
curl --fail --silent --show-error --location \
  "https://github.com/rhysd/actionlint/releases/download/v1.7.12/$archive" \
  --output "$tool_dir/$archive"
printf '%s  %s\n' \
  8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8 \
  "$tool_dir/$archive" | sha256sum --check --status
tar -xzf "$tool_dir/$archive" -C "$tool_dir" actionlint
"$tool_dir/actionlint" -shellcheck= -pyflakes=

for metadata in release-please-config.json .release-please-manifest.json; do
  if [[ -f "$metadata" ]]; then
    python3 -m json.tool "$metadata" >/dev/null
  fi
done
bash -n .github/scripts/check-workflows.sh
