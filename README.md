# Java OpenDiscogs Batch

Stream Discogs monthly public data dumps into PostgreSQL with Spring Batch,
bounded memory, and resumable, idempotent imports.

Java and Go consume the same canonical schema published by
[`open-discogs-model`](https://github.com/dsub-io/open-discogs-model). This is
an independent project and is not endorsed by Discogs.

- [Import safety and recovery](docs/import-safety.md)
- [Performance measurements](docs/performance.md)
- [Releases](https://github.com/dsub-io/open-discogs-batch/releases)

## Quick start

The PostgreSQL database must already exist. The importer creates the selected
schema when permitted, applies canonical migrations, downloads the dumps, and
imports them.

```shell
java -jar open-discogs-batch-*.jar \
  --database-url 'postgresql://user:password@localhost:5432/open_discogs' \
  --database-schema open_discogs \
  --entities artist,label,master,release
```

Omit `--dump-month` to select the latest dump for each entity independently.
Use `--dump-month=2026-07` when every selected entity must come from that month.

## What an import guarantees

- Artist, label, master, and release dumps are decompressed and parsed as
  streams; the full dump is never held in memory.
- A compatible interrupted run resumes committed relation chunks; safe core
  phases rerun. `--force` starts the same manifest from zero.
- Canonical relation changes and their durable progress commit atomically.
- Removed relations are reconciled; roots absent from a newer dump are not
  deleted.
- Older dumps require `--allow-downgrade`. Downloads are retained unless a
  successful run uses `--cleanup`.

See [Import safety and recovery](docs/import-safety.md) for exact discovery,
locking, interruption, convergence, and snapshot-visibility semantics.

## Database setup

There is no separate `init` command and the database itself is never created.
The importer executes migrations packaged by its `open-discogs-model`
dependency.

| Target state | Required authority |
| --- | --- |
| Schema does not exist | `CREATE` on the database |
| Schema exists | `USAGE` and `CREATE` on the schema, table writes, and migration DDL authority |
| Batch role cannot migrate | A DBA must prepare the schema and grants first |

The default schema is `public` and produces a warning on every startup. Prefer
a dedicated schema such as `open_discogs`. Schema names must be 1–63 lowercase
letters, digits, or underscores and begin with a letter or underscore.

## Configuration

Precedence is `CLI > ENV > default`.

| CLI | Environment | Default | Purpose |
| --- | --- | --- | --- |
| `--database-url` | `OPEN_DISCOGS_BATCH_DATABASE_URL` | required | PostgreSQL URI; percent-encode credentials |
| `--database-schema` | `OPEN_DISCOGS_BATCH_DATABASE_SCHEMA` | `public` | Schema to create or migrate |
| `--entities`, `-e` | `OPEN_DISCOGS_BATCH_ENTITIES` | all | Comma-separated entity list |
| `--dump-month`, `-m` | `OPEN_DISCOGS_BATCH_DUMP_MONTH` | latest per entity | Exact month in `yyyy-MM` |
| `--data-dir` | `OPEN_DISCOGS_BATCH_DATA_DIR` | `~/.cache/open-discogs-batch` | Download directory |
| `--chunk-size`, `-b` | `OPEN_DISCOGS_BATCH_CHUNK_SIZE` | `5000` | Roots per transaction chunk |
| `--max-workers` | `OPEN_DISCOGS_BATCH_MAX_WORKERS` | runtime CPU allocation | Concurrent import workers |
| `--cleanup`, `-c` | `OPEN_DISCOGS_BATCH_CLEANUP` | `false` | Remove selected dumps after success |
| `--force`, `-f` | `OPEN_DISCOGS_BATCH_FORCE` | `false` | Reprocess a successful manifest |
| `--allow-downgrade` | `OPEN_DISCOGS_BATCH_ALLOW_DOWNGRADE` | `false` | Permit and audit older dumps |
| `--help`, `-h` | — | — | Show help without connecting to PostgreSQL |
| `--version`, `-v` | — | — | Show version without connecting to PostgreSQL |

Keep credentials out of command history and inject the database URI through
your platform's secret mechanism.

## Operate it

### Progress output

An interactive console gets one progress bar. Redirected output, pipelines,
containers, and Kubernetes suppress carriage-return bars and retain structured
logs. Import counters represent durably committed roots; a percentage appears
only after the exact stream total has been established.

### CPU and memory

`--max-workers` limits importer concurrency, not CPU usage. Omitted means the
JVM's visible processor allocation; no percentage heuristic is applied. Use
container or scheduler CPU limits for a hard quota.

Working memory is governed by `chunk-size × max-workers × relation fan-out`.
Release has the highest fan-out. Start with workers no higher than the CPU and
database connections reserved for the job, then reduce chunk size for memory
pressure or workers for database contention. See [Performance measurements](docs/performance.md)
for measured results and reproduction commands.

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

Mount writable storage when downloads must survive container replacement.
Versioned executable JARs are attached to GitHub Releases. Build a local image
with `./gradlew bootBuildImage`.

## Development

Source builds require JDK 21. The `.sdkmanrc` selects Temurin 21.0.11.
Integration tests require Docker and remove the exact containers, networks, and
volumes they create on success or failure.

```shell
sdk env
./gradlew clean check --no-daemon --warning-mode=fail
./gradlew e2eTest --no-daemon --warning-mode=fail
```

CI verifies deterministic unit, PostgreSQL integration, and dump E2E behavior,
plus 100% line and branch coverage.

## License

MIT. See [LICENSE](LICENSE). Retain the `state303` attribution required by the
license.
