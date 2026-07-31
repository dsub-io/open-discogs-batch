# OpenDiscogs Batch

OpenDiscogs Batch imports the public OpenDiscogs monthly dumps into PostgreSQL.
It uses Spring Batch for orchestration and the canonical jOOQ schema model from
[`open-discogs-model`](https://github.com/dsub-io/open-discogs-model), so the
Java and Go importers write the same database contract.

This is an independent DSUB project. It is not affiliated with or endorsed by
Discogs. The Discogs name identifies only the public data source.

## Import behavior and safety

- Liquibase schema migrations and dump discovery run automatically.
- Artist, label, master, and release select their newest available dump
  independently unless an exact `--dump-month` is requested.
- Every run records the selected dump dates, SHA-256 checksums, source URIs,
  sizes, and stable identifiers as one immutable manifest.
- A manifest that already succeeded is skipped. `--force` reruns that same
  manifest without changing the idempotent database result.
- An older entity dump is rejected unless `--allow-downgrade` is supplied; the
  override is recorded in import history.
- PostgreSQL advisory locks prevent concurrent runs from updating an overlapping
  entity set. Runs with disjoint entity sets may proceed together.
- Downloads are retained by default. `--cleanup` deletes them only after a
  successful import. Failed imports retain their files for retry.

Discogs dump paths such as `data/2026/discogs_20260701_releases.xml.gz` are used
as stable identifiers and are paired with the same date's checksum manifest.
When one domain is absent from a month, it does not roll the other domains back.

## Requirements

- JDK 21
- A reachable PostgreSQL database
- Docker for Testcontainers-based integration and E2E tests

The repository includes an `.sdkmanrc` pinned to Temurin 21.0.11:

```shell
sdk env
```

The build uses Gradle 9.6.1, Spring Boot 4.1, and Spring Batch 6. Dependencies
resolve from Maven Central, including
`io.dsub.opendiscogs:open-discogs-model-jooq:0.1.2`. No GitHub token is required
to build or run this project.

## Build and test

```shell
./gradlew clean assemble
./gradlew clean check --no-daemon --warning-mode=fail
./gradlew e2eTest --no-daemon --warning-mode=fail
```

`check` runs the deterministic unit and integration suite, generates JaCoCo
reports, enforces 85% line and 40% branch coverage, and validates test naming.
`e2eTest` imports the complete cross-language fixture into PostgreSQL and
verifies reruns and entity admission rules. CI uses GitHub-hosted
`ubuntu-latest` and does not depend on live Discogs availability.

## Usage

```shell
java -jar build/libs/open-discogs-batch-0.1.8.jar \
  --database-url 'postgresql://user:password@localhost:5432/open_discogs' \
  --entities artist,label,master,release
```

Use `--dump-month=2026-07` to require that exact month. Without it, each selected
entity uses its own latest available dump.

| Option | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `--database-url` | `OPEN_DISCOGS_BATCH_DATABASE_URL` | required | PostgreSQL URI including percent-encoded credentials |
| `--entities`, `-e` | `OPEN_DISCOGS_BATCH_ENTITIES` | all four | Comma-separated `artist`, `label`, `master`, `release` |
| `--dump-month`, `-m` | `OPEN_DISCOGS_BATCH_DUMP_MONTH` | latest per entity | Exact dump month in `yyyy-MM` form |
| `--data-dir` | `OPEN_DISCOGS_BATCH_DATA_DIR` | `~/.cache/open-discogs-batch` | Download directory |
| `--chunk-size`, `-b` | `OPEN_DISCOGS_BATCH_CHUNK_SIZE` | `5000` | Import chunk size |
| `--cleanup`, `-c` | `OPEN_DISCOGS_BATCH_CLEANUP` | `false` | Delete downloads after success |
| `--force`, `-f` | `OPEN_DISCOGS_BATCH_FORCE` | `false` | Rerun an already-successful manifest |
| `--allow-downgrade` | `OPEN_DISCOGS_BATCH_ALLOW_DOWNGRADE` | `false` | Permit and audit older entity dumps |
| `--help`, `-h` | — | — | Show help |
| `--version`, `-v` | — | — | Show version |

Command-line options take precedence over environment variables, which take
precedence over defaults. The two importer implementations accept this same
public contract. The former `url`, `username`, `password`, `type`, `year`,
`yearMonth`, `eTag`, `mount`, `strict`, `coreCount`, and driver override options
are no longer part of the public interface.

Percent-encode reserved characters in the URI username or password. Never
commit a real database URL to source control. Environment variables also remain
visible to a container administrator, so provide them through the deployment
platform's secret mechanism.

## Container image

Build a local OCI image with the Spring Boot buildpack task:

```shell
./gradlew bootBuildImage
docker run --rm \
  -e OPEN_DISCOGS_BATCH_DATABASE_URL='postgresql://user:password@db:5432/open_discogs' \
  -e OPEN_DISCOGS_BATCH_ENTITIES='artist,label,master,release' \
  -e OPEN_DISCOGS_BATCH_DATA_DIR=/data \
  -v open-discogs-data:/data \
  open-discogs-batch:0.1.8
```

Setting `OPEN_DISCOGS_BATCH_CLEANUP=true` removes downloads only after success.

## Contributing

Pull request titles and commit subjects must follow Conventional Commits.
Allowed types are `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`,
`refactor`, `revert`, `style`, and `test`. Branches must not use `agent/`,
`codex/`, or `claude/` prefixes.

## License

MIT. See [LICENSE](LICENSE). The `state303` attribution must be retained in
copies or substantial portions of the software.
