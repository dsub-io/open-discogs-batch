# OpenDiscogs Batch

OpenDiscogs Batch imports the public OpenDiscogs monthly dumps into PostgreSQL.
It uses Spring Batch for orchestration and the canonical jOOQ schema model from
[`open-discogs-model`](https://github.com/dsub-io/open-discogs-model), so the
Java and Go importers write the same database contract.

This is an independent DSUB project. It is not affiliated with or endorsed by
Discogs. The Discogs name identifies only the public data source.

## Import behavior and safety

- The selected database schema, Liquibase migrations, tables, indexes, and dump discovery run automatically.
- Artist, label, master, and release select their newest available dump
  independently unless an exact `--dump-month` is requested.
- Every run records the selected dump dates, SHA-256 checksums, source URIs,
  sizes, and stable identifiers as one immutable manifest.
- A manifest that already succeeded is skipped. `--force` reruns that same
  manifest without changing the idempotent database result.
- A failed or abandoned run resumes committed source chunks only when its
  manifest, processor version, entity set, dump identities, and chunk size all
  match. `--force` always starts a fresh run.
- Canonical relation changes and their source-chunk ledger entry commit in one
  PostgreSQL transaction. A retry parses the stream from the beginning but
  skips the exact relation chunks already committed; core rows and any work
  after the relation phase are safely rerun.
- Relation rows missing from a newer representation of a root are deleted while
  unchanged rows retain their surrogate IDs. Roots absent from the entire dump
  are not deleted automatically.
- The v1 schema still identifies several relation values with a 32-bit Java
  hash. A collision within one root can merge distinct values; the measured,
  online migration to collision-resistant identity is tracked in
  [`open-discogs-model#43`](https://github.com/dsub-io/open-discogs-model/issues/43).
- An older entity dump is rejected unless `--allow-downgrade` is supplied; the
  override is recorded in import history.
- PostgreSQL advisory locks cover both selected entities and their reference
  dependencies. Master locks Artist and Master; Release locks Artist, Label,
  Master, and Release because it also updates `master.main_release_id`.
  Independent sets such as Artist and Label may still proceed together.
- Downloads are retained by default. `--cleanup` deletes them only after a
  successful database import. A cleanup failure is reported without
  reclassifying the committed import, and the next invocation retries cleanup
  through the successful-manifest skip path. Failed imports retain their files
  for retry.

The durability boundary is one source chunk, not the whole monthly snapshot.
Readers can observe already-committed chunks while an import is running. Use a
versioned database or replica promotion when consumers require all entities to
switch snapshots atomically.

### Progress observability

Download bars use the dump catalog's compressed size. `SOURCE READ` bars use the
exact local compressed file size and report byte progress, throughput, elapsed
time, and source-read ETA. They describe how much of the selected `.xml.gz` file
has been consumed, not how many PostgreSQL rows have committed.

When the process is attached to an interactive console, the progress bar is the
only live progress display. In non-interactive executions such as redirected
output, pipelines, containers, and Kubernetes, carriage-return bars are
suppressed and structured progress logs remain enabled.

In non-interactive executions, each entity emits structured
`event=import_progress` records at start,
completion, failure, and at most once every five seconds while chunks commit.
The records include exact durable `committed_items`, `rows_per_second`, elapsed
time, resume state, initial committed items, and the latest committed progress
timestamp. Observation failures are warnings and do not change import success.
Each emitted sample performs one primary-key summary read, bounded to 0.2 reads
per second per active entity plus the start and finish reads; it never scans the
chunk ledger or entity tables.

`committed_percent` is `unavailable` until end-of-stream coverage validation
establishes the exact entity total; a completed entity reports `100.00`. The
importer deliberately does not pre-scan a dump just to count roots, because
that would add a complete gzip/XML pass before a 200-million-row import. It also
does not synthesize one percentage across entity types whose parse and relation
costs differ. A resumed entity whose coverage was already complete can start at
100% while the source is reread to validate and skip committed chunks.

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
`io.dsub.opendiscogs:open-discogs-model-jooq:0.2.2`. No GitHub token is required
to build or run this project.

## Build and test

```shell
./gradlew clean assemble
./gradlew clean check --no-daemon --warning-mode=fail
./gradlew e2eTest --no-daemon --warning-mode=fail
```

`check` runs the deterministic unit, integration, and E2E suites, generates a
combined JaCoCo report, enforces 100% line and branch coverage, and validates
test naming. `e2eTest` imports the complete cross-language fixture into
PostgreSQL and verifies reruns and entity admission rules. CI uses
GitHub-hosted `ubuntu-latest` and does not depend on live Discogs availability.

## Usage

```shell
java -jar build/libs/open-discogs-batch-*.jar \
  --database-url 'postgresql://user:password@localhost:5432/open_discogs' \
  --database-schema open_discogs \
  --entities artist,label,master,release
```

Use `--dump-month=2026-07` to require that exact month. Without it, each selected
entity uses its own latest available dump.

The PostgreSQL database itself must already exist. A normal batch run needs no separate `init` command: it creates `--database-schema` when missing, then creates or migrates the canonical tables inside it. Creating a missing schema requires `CREATE` on the target database. For an existing schema, the batch role requires `USAGE` and `CREATE`, write access to imported tables, and ownership or equivalent DDL authority for migrations. If those privileges are intentionally unavailable, pre-create the schema with a DBA-managed role and grant the batch role the required schema and table privileges before running the importer.

When `--database-schema` / `OPEN_DISCOGS_BATCH_DATABASE_SCHEMA` is omitted, the importer uses `public` and emits a `WARN` on every startup. This is convenient for compatibility but can mix OpenDiscogs objects with unrelated public tables, so a dedicated name such as `open_discogs` is recommended. Schema names use portable PostgreSQL identifiers: 1–63 lowercase letters, digits, or underscores, starting with a letter or underscore.

| Option | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `--database-url` | `OPEN_DISCOGS_BATCH_DATABASE_URL` | required | PostgreSQL URI including percent-encoded credentials |
| `--database-schema` | `OPEN_DISCOGS_BATCH_DATABASE_SCHEMA` | `public` | Schema to create or migrate; `public` emits a startup warning |
| `--entities`, `-e` | `OPEN_DISCOGS_BATCH_ENTITIES` | all four | Comma-separated `artist`, `label`, `master`, `release` |
| `--dump-month`, `-m` | `OPEN_DISCOGS_BATCH_DUMP_MONTH` | latest per entity | Exact dump month in `yyyy-MM` form |
| `--data-dir` | `OPEN_DISCOGS_BATCH_DATA_DIR` | `~/.cache/open-discogs-batch` | Download directory |
| `--chunk-size`, `-b` | `OPEN_DISCOGS_BATCH_CHUNK_SIZE` | `5000` | Import chunk size |
| `--max-workers` | `OPEN_DISCOGS_BATCH_MAX_WORKERS` | runtime CPU allocation | Maximum concurrent import workers |
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

`--max-workers` is the exact upper bound on application-managed concurrent
import workers. When omitted, it resolves to the processor allocation visible
to the runtime; no percentage or physical-core heuristic is applied. It is not
a hard CPU quota. Use the container or workload scheduler's CPU limit when the
process itself must not exceed a CPU allocation.

## Large-import resource model

The dump is decompressed and parsed as a stream; it is never loaded into memory
as one document. Worker submission has no waiting queue: when all workers are
busy, XML production waits until a worker becomes available. The live chunk
count therefore cannot grow with the total dump size.

Integer reference IDs use a segmented concurrent bit set. Each occupied range
of 65,536 IDs allocates 8 KiB of bit storage, so dense IDs use one bit each.
When an import depends on entities not selected in the same run, their IDs are
read from PostgreSQL with a 10,000-row server-side cursor rather than fetched as
one in-memory result. Expanded relation records are also flushed to jOOQ in
`chunk-size` batches instead of accumulating one unbounded JDBC batch.

Peak working memory is driven by `chunk-size × max-workers × relation fan-out`,
not by the total row count. Release records have the largest fan-out. For a
large production import, set `--max-workers` explicitly to the smaller of the
container CPU allocation and the number of database write connections reserved
for this job, then tune `--chunk-size` separately from measured heap usage and
database latency. Lower `chunk-size` before lowering worker count when a single
release chunk is too large; lower `max-workers` when concurrent chunks or the
database are the constraint.

### Measured ID-cache improvement

On an Apple M2 Pro with Java 21, the previous skip-list cache and two inversion
passes were compared with the segmented bit set using the same sequence of
1,000,000 positive IDs. Across three fresh-process runs, median elapsed time
fell from 648.549 ms to 28.997 ms (`22.4×` faster), and median maximum RSS fell
from 163.9 MB to 64.47 MB (`60.7%` lower). The new bit-set words occupied 128
KiB. These figures isolate the ID-cache operation; end-to-end import throughput
still depends on dump shape, PostgreSQL, storage, and runtime limits.

### Measured durable-import cost

The forced idempotency path was measured against `v1.0.0` and this change on
the same Apple M2 Pro, Java 21, PostgreSQL 18.4 Alpine tmpfs container, four
3-record fixture dumps, `chunk-size=1000`, one worker, two consecutive imports
per sample, two warm-ups, and 20 fresh test samples. The elapsed time for both
imports changed from p50/p95/p99 `734/845/854 ms` to `767/916/980 ms`
(`4.5%/8.4%/14.8%` higher). Median throughput changed from `32.7` to `31.3`
records/s (`4.3%` lower).

The added cost covers active-run fencing, exact source-chunk ledger commits,
coverage validation, and stale-relation reconciliation. This 24-record test
exaggerates fixed transaction cost and is not a 200-million-row throughput
estimate. RSS and allocation deltas are not reported because the isolated test
process includes Gradle and Testcontainers while the fixture is too small to
represent production heap use. The implementation instead bounds live source
chunks to `chunk-size × max-workers`; a representative production dump still
needs a heap/RSS profile on the target hardware before sizing.

Reproduce the latency sample with:

```shell
./gradlew cleanTest test \
  --tests 'io.dsub.discogs.batch.job.PostgreSQLDiscogsJobIntegrationTest.whenSameDumpIsForcedTwice__BusinessRowsRemainIdentical' \
  --quiet
```

Percent-encode reserved characters in the URI username or password. Never
commit a real database URL to source control. Environment variables also remain
visible to a container administrator, so provide them through the deployment
platform's secret mechanism.

## Container

Release images are published from Release Please release commits for
`linux/amd64` and `linux/arm64`:

```shell
docker pull ghcr.io/dsub-io/open-discogs-batch:latest
docker run --rm \
  --cpus=4 \
  -e OPEN_DISCOGS_BATCH_DATABASE_URL='postgresql://user:password@db:5432/open_discogs' \
  -e OPEN_DISCOGS_BATCH_DATABASE_SCHEMA='open_discogs' \
  -e OPEN_DISCOGS_BATCH_ENTITIES='artist,label,master,release' \
  -e OPEN_DISCOGS_BATCH_DATA_DIR=/data \
  -e OPEN_DISCOGS_BATCH_MAX_WORKERS=4 \
  -v open-discogs-data:/data \
  ghcr.io/dsub-io/open-discogs-batch:latest
```

The image runs as a non-root user. Mount a writable volume when downloads must
survive container removal. Setting `OPEN_DISCOGS_BATCH_CLEANUP=true` removes
downloads only after success. Versioned executable JARs are attached to the
repository's GitHub Releases.

For a local buildpack image instead of the published Docker image:

```shell
./gradlew bootBuildImage
```

## Contributing

Pull request titles and commit subjects must follow Conventional Commits.
Allowed types are `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`,
`refactor`, `revert`, `style`, and `test`. Branches must not use `agent/`,
`codex/`, or `claude/` prefixes.

## License

MIT. See [LICENSE](LICENSE). The `state303` attribution must be retained in
copies or substantial portions of the software.
