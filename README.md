# Java OpenDiscogs Batch

Stream Discogs monthly public data dumps into PostgreSQL with Spring Batch,
bounded memory, durable progress, and idempotent recovery.

This release consumes canonical
[`open-discogs-model`](https://github.com/dsub-io/open-discogs-model) v0.3.1.
Java and Go therefore apply the same migration bytes and import contracts. This
is an independent project and is not endorsed by Discogs.

> [!CAUTION]
> **Production import is not approved yet.** Publish both batch implementations
> against model v0.3.1 and complete cross-language migration, recovery, and
> full-dump validation before starting or resuming a production import.

- [Import safety and recovery](docs/import-safety.md)
- [Performance measurements](docs/performance.md)
- [Releases](https://github.com/dsub-io/open-discogs-batch/releases)

## Quick start

The PostgreSQL database must already exist. The importer creates the selected
schema when permitted, applies canonical migrations, resolves and downloads the
selected dumps, then imports them.

```shell
java -jar open-discogs-batch-*.jar \
  --database-url 'postgresql://user:password@localhost:5432/open_discogs' \
  --database-schema open_discogs \
  --entities artist,label,master,release
```

Omit `--dump-month` to select the latest dump for each entity independently.
Use `--dump-month=2026-07` to require every selected entity from that month.
Exact-month runs use a complete durable catalog without an upstream request;
otherwise discovery performs the bounded request sequence documented in
[Import safety and recovery](docs/import-safety.md#catalog-request-budget).

## Import contract

| Boundary | Behavior |
| --- | --- |
| Source | Monthly public dumps only; no Discogs API, live hydration, or user writes |
| Memory | Dumps are decompressed and parsed as streams; the full dump is never held in memory |
| Commit | Each Release root, genre/style dictionaries, supported relations, master assignment, and durable progress commit atomically |
| Retry | Compatible interrupted runs resume verified chunks; non-Release safe core phases may rerun; `--force` restarts the same manifest from zero |
| Convergence | Supported missing relations are removed; roots absent from a newer dump are not deleted |
| Visibility | Readers can observe committed chunks; a complete monthly import is not an atomic snapshot switch |
| Files | Failed-run downloads remain; `--cleanup` removes only this run's selected files after success |

Older dumps require `--allow-downgrade`. See
[Import safety and recovery](docs/import-safety.md) for admission, locking,
interruption, and resume rules.

### Release data scope

| Release data | Current behavior |
| --- | --- |
| Imported | Core fields; release artists; labels and catalog numbers; companies; formats; genres and styles; identifiers; top-level tracks; videos; release-level credited artist ID and role |
| Not imported | Series membership; per-track artists and extra artists; sub-track/index-track hierarchy; `anv`, `join`, and credit `tracks` metadata not represented by the canonical schema |
| Images | The audited 2026-08 public release dump has no image elements; no separate image source is used |

Downstream services must review the current
[Discogs API Terms of Use](https://support.discogs.com/hc/en-us/articles/360009334593-API-Terms-of-Use)
for every source they combine. A monthly snapshot cannot satisfy live API
freshness by itself; attribution, refresh, caching, and redistribution remain
the downstream operator's responsibility.

## Database setup

PostgreSQL 15 or newer is required. There is no separate `init` command, and
the importer never creates the database.

| Target | Required authority |
| --- | --- |
| Missing schema | `CREATE` on the database; the importer creates the selected schema |
| Existing schema | `USAGE` and `CREATE` on the schema, table writes, and migration DDL authority |
| Restricted batch role | A DBA prepares the schema, extension, and grants first |

Model v0.3.1 migrations packaged in the model dependency are the only schema
source of truth. Migration V007 uses
`CREATE EXTENSION IF NOT EXISTS pg_trgm`; allow the migration role to install
this trusted extension or have a DBA pre-install it in a stable schema visible
to the migration role. Catalog tables are still created only in the selected
database schema.

> [!WARNING]
> Omitting `--database-schema` uses `public` and emits a warning on every
> startup. Prefer a dedicated schema such as `open_discogs`.

Schema names must be 1–63 lowercase letters, digits, or underscores and begin
with a letter or underscore.

## Configuration

Precedence is `CLI > ENV > default`.

| CLI | Environment | Type | Default | Required | Valid values / purpose |
| --- | --- | --- | --- | --- | --- |
| `--database-url` | `OPEN_DISCOGS_BATCH_DATABASE_URL` | URI | none | yes | `postgres[ql]://user:password@host[:port]/database`; percent-encode credentials |
| `--database-schema` | `OPEN_DISCOGS_BATCH_DATABASE_SCHEMA` | string | `public` | no | 1–63 lowercase letters, digits, or underscores; starts with a letter or underscore |
| `--entities`, `-e` | `OPEN_DISCOGS_BATCH_ENTITIES` | string list | all four | no | Non-empty subset of `artist,label,master,release` |
| `--dump-month`, `-m` | `OPEN_DISCOGS_BATCH_DUMP_MONTH` | `yyyy-MM` | latest per entity | no | `2008-03` through the current calendar month |
| `--data-dir` | `OPEN_DISCOGS_BATCH_DATA_DIR` | path | `~/.cache/open-discogs-batch` | no | Writable download directory |
| `--chunk-size`, `-b` | `OPEN_DISCOGS_BATCH_CHUNK_SIZE` | integer | `5000` | no | `1..2,147,483,647`; roots per transaction chunk |
| `--max-workers` | `OPEN_DISCOGS_BATCH_MAX_WORKERS` | integer | visible CPU count | no | `1..2,147,483,647`; concurrent import workers |
| `--cleanup`, `-c` | `OPEN_DISCOGS_BATCH_CLEANUP` | boolean | `false` | no | Remove only selected dumps after successful import |
| `--force`, `-f` | `OPEN_DISCOGS_BATCH_FORCE` | boolean | `false` | no | Reprocess an otherwise skippable successful manifest from zero |
| `--allow-downgrade` | `OPEN_DISCOGS_BATCH_ALLOW_DOWNGRADE` | boolean | `false` | no | Permit and audit an older dump than the entity checkpoint |
| `--help`, `-h` | — | action | `false` | no | Show help without connecting to PostgreSQL |
| `--version`, `-v` | — | action | `false` | no | Show version without connecting to PostgreSQL |

Boolean ENV values accept `true/false`, `1/0`, `yes/no`, and `on/off`
case-insensitively. `OPEN_DISCOGS_BATCH_ENTITIES` is comma-separated. Inject
the database URI through a secret mechanism instead of command history.

## Operations

### Progress output

| Runtime | Output |
| --- | --- |
| Interactive system console | One source-read byte-progress bar on stderr; structured progress logs are suppressed |
| Non-interactive run | No bar; ordinary SLF4J key-value records such as `event=import_progress state=running` |

The key-value records are not JSON. They report durably committed roots rather
than merely parsed rows; `committed_percent=unavailable` remains until an exact
entity total is stored. Default logging is console-only. Set Spring's standard
`LOGGING_FILE_NAME` only when file logging is explicitly required.

### Resources

`--max-workers` limits import concurrency, not CPU usage. The default is the
JVM's visible processor count; no percentage heuristic is applied. Use
container or scheduler limits for a hard CPU quota.

Working memory grows with `chunk-size × max-workers × relation fan-out`.
Release has the highest fan-out. Hikari allows at most `max-workers + 3` open
connections. Agree that budget with the DBA and reduce chunk size or workers
under pressure.

### Container

The non-root image supports `linux/amd64` and `linux/arm64`.

```shell
docker run --rm --cpus=4 \
  -e OPEN_DISCOGS_BATCH_DATABASE_URL='postgresql://user:password@db:5432/open_discogs' \
  -e OPEN_DISCOGS_BATCH_DATABASE_SCHEMA=open_discogs \
  -e OPEN_DISCOGS_BATCH_ENTITIES=artist,label,master,release \
  -e OPEN_DISCOGS_BATCH_DATA_DIR=/data \
  -e OPEN_DISCOGS_BATCH_MAX_WORKERS=4 \
  -v open-discogs-data:/data \
  ghcr.io/dsub-io/open-discogs-batch:latest
```

Mount writable storage only when downloads must survive container replacement.
Versioned executable JARs are attached to GitHub Releases. Build a local image
with `./gradlew bootBuildImage`.

## Development

Source builds require JDK 21; `.sdkmanrc` selects Temurin 21.0.11. Integration
tests label every owned Docker resource with a per-run identity and use tmpfs
for PostgreSQL. In-process cleanup and CI's always-run teardown remove only
those containers, networks, and volumes, then verify that residue is zero.
Persistent test volumes and bind mounts are not allowed.

```shell
sdk env
./gradlew test             # pure unit tests; does not start Docker
./gradlew integrationTest  # PostgreSQL and adapter contracts
./gradlew e2eTest          # deterministic dump-to-PostgreSQL flows
./gradlew clean check --no-daemon --warning-mode=fail
```

`check` runs all three lanes, naming validation, and 100% line and branch
coverage. Integration and E2E tests share their PostgreSQL fixture within each
JVM and clean every owned Docker resource after the lane completes.

## License

MIT. See [LICENSE](LICENSE). Retain the `state303` attribution required by the
license.
