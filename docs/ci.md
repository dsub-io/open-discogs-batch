# CI and contributions

The Go migration notice workflow updates the repository's description and
website and prepends a migration link to published Java release notes. It runs
when its configuration changes on `main`, when a release is published, or
manually. It uses the existing `GH_TOKEN` secret and runs no application tests.
Original release notes and assets are retained.

Application verification runs on pull requests and merge-queue candidates.
Merging into `main` does not rerun the CI workflow. The separate release
workflow still handles release metadata and verifies artifacts when a release
is created. Scheduled verification, where configured, and manual CI runs check
the full project.

| Change | Verification |
| --- | --- |
| Root Markdown, prose or images under `docs/`, license text | Diff whitespace and contribution checks |
| `.github/` or Release Please metadata | CI routing regression tests, workflow syntax and JSON validation; no application suite |
| Source, tests, dependencies, build scripts, container or deployment configuration | Full application verification |
| Canonical schema, including Markdown under `schema/contracts/` | Full application verification |
| Mixed documentation and application changes | Full application verification |
| PR title or body edit | Separate contribution workflow, where configured; no application suite |
| PR retargeted to a different base | Classify the new diff and verify affected files |

Classification uses changed paths, not commit prefixes. A `docs:` or `ci:`
commit that changes application code still runs application checks. Files not
recognized as prose or CI metadata also run application checks.

Title and body edits use a separate concurrency group and a `PR metadata`
check name. They cannot cancel or replace the required application check. The separate
contribution workflow rechecks Conventional Commits without cancelling or
replacing the application check.

The workflow starts even for documentation-only PRs, so its check can finish
successfully without leaving a required check pending. Renames and deletions
are included. PR diffs start at the merge base, so unrelated changes already
merged into the base branch do not trigger application tests.

Run the lightweight CI routing tests locally with Python 3:

```sh
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
```

Validate workflow YAML with [actionlint](https://github.com/rhysd/actionlint).
The Linux CI helper pins version 1.7.12 and verifies its archive checksum.

Use [Conventional Commits](https://www.conventionalcommits.org/) for PR titles
and commit subjects. Use `docs:` for documentation and `ci:` for workflow
changes; release behavior is configured separately by each repository.
