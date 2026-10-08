# Contributor instructions

Read `README.md` and `docs/ci.md` before changing documentation or workflows.

## Writing

Use the `humanizer` skill for README files, documentation, PR descriptions,
and release notes when it is available. Return the final text without the
intermediate draft or critique unless requested.

Lead with the information the reader needs. Use ordinary words and exact
technical terms. Remove promotional claims, repeated conclusions, invented
contrasts, and decorative formatting. Keep useful tables and lists. Preserve
facts, conditions, uncertainty, attribution, and the author's voice. Do not
invent performance figures, support promises, or personal experiences.

When editing prose, preserve commands, code, configuration names, versions,
and link targets unless the task requires changing them. Verify any such
change against the source. Apply these principles to conversation replies too.

## Project direction

This Java importer is deprecated. Direct users to its Go replacement and
the migration guide before Java setup instructions.

Recommend `dsub-io/go-open-discogs-api` and `dsub-io/go-open-discogs-batch`
for new deployments. Java and historical schema documentation must clearly
identify their role. Canonical migrations live in `open-discogs-model`.

## CI

Keep application checks on PRs and merge-queue candidates. Documentation and
CI-only edits use lightweight checks. Treat packaged schema contracts as
application inputs even when their files end in `.md`. Preserve release
publication and artifact verification when changing CI routing.
