# OpenDiscogs Batch

OpenDiscogs Batch imports the public monthly data dumps from
[data.discogs.com](https://data.discogs.com) into PostgreSQL. It uses Spring
Batch for orchestration, Liquibase for schema setup, and jOOQ for database
writes.

This is an independent DSUB project. It is not affiliated with or endorsed by
Discogs. The Discogs name is used only to identify the public data source.

## What it does

- Downloads a selected dump, or resolves the most recent available dump.
- Verifies downloaded dumps against the matching Discogs SHA-256 manifest.
- Creates and updates the PostgreSQL schema through Liquibase.
- Imports artist, label, master, and release data in dependency order.
- Uses idempotent writes so a dump can be processed again.
- Can retain downloaded dump files when `mount` is enabled.

The current entity order is:

```text
artist -> label -> master -> release
```

Selecting `master` also selects artist and label dependencies. Selecting
`release` selects all four entity types unless `strict` is enabled.

Each requested entity independently selects its newest available dump. A full
import can therefore combine, for example, a newer artist dump with an older
release dump when an upstream publication omits one domain. Each entity keeps
its own successful checkpoint and exact source provenance.

The current schema is documented in the
[OpenDiscogs ERD](https://dbdocs.io/state303/OpenDiscogs).

## Requirements

- JDK 21
- A reachable PostgreSQL database
- Network access to `data.discogs.com`
- Docker for the Testcontainers-based integration tests

The repository includes an `.sdkmanrc` pinned to Temurin 21.0.11. With SDKMAN
installed, activate it with:

```bash
sdk env
```

The Gradle wrapper uses Gradle 9.6.1. The application is built on Spring Boot
4.1 and Spring Batch 6.

Dependencies resolve from Maven Central. The generated schema library is
published as:

```text
io.dsub.opendiscogs:open-discogs-model-jooq:0.1.2
```

No GitHub token is required to build or run this project.

## Build and test

Build the executable JAR:

```bash
./gradlew clean assemble
```

Run the deterministic unit and integration suite, generate the coverage report,
and enforce the coverage gate:

```bash
./gradlew clean check
```

Integration tests start PostgreSQL through Testcontainers and therefore require
Docker. They also exercise discovery, downloads, checksums, repository
selection, and independent per-entity dates against a loopback distribution
fixture.

The end-to-end lane makes one bounded request for the public July 2026 checksum
manifest and verifies that it advertises all four entity dumps:

```bash
./gradlew e2eTest
```

The E2E workflow runs on every pull request and on manual dispatch using a
GitHub-hosted `ubuntu-latest` runner. It does not use a self-hosted runner and
does not download the large XML dumps.

Executable test classes use one of three suffixes:

- `*UnitTest` for isolated tests.
- `*IntegrationTest` for deterministic component and Testcontainers tests.
- `*E2ETest` for opt-in end-to-end checks against external services.

The build rejects other executable test class names. Pull requests must pass
the deterministic suite, the live Discogs E2E suite, and maintain at least 85%
line coverage and 40% branch coverage.

The executable is written to:

```text
build/libs/open-discogs-batch-0.1.8.jar
```

## Run

The database URL, username, and password are required. For example:

```bash
java -jar build/libs/open-discogs-batch-0.1.8.jar \
  --url=jdbc:postgresql://localhost:5432/discogs \
  --username=<database-user> \
  --password=<database-password> \
  --yearMonth=2021-03 \
  --type=release
```

Options may be written with `--`, `-`, or no leading hyphen. Comma-separated
values are expanded into individual option values.

| Option | Aliases | Required | Purpose |
| --- | --- | --- | --- |
| `url` | — | yes | PostgreSQL JDBC URL |
| `username` | `user`, `u` | yes | Database user |
| `password` | `pass`, `p` | yes | Database password |
| `type` | `t` | no | `artist`, `label`, `master`, or `release` |
| `year` | `y` | no | Dump year |
| `yearMonth` | `ym` | no | Dump month in `yyyy-MM` form |
| `eTag` | `e` | no | Stable dump ID; overrides year, month, and type selection |
| `chunkSize` | `chunk`, `c` | no | Spring Batch chunk size; defaults to `500` |
| `coreCount` | `core` | no | Worker count; defaults to 80% of physical cores |
| `mount` | `m` | no | Keep downloaded dump files |
| `strict` | `s` | no | Do not add entity dependencies |
| `force` | `f` | no | Reprocess an already successful manifest |
| `allowDowngrade` | `allow-downgrade` | no | Explicitly allow an older dump than the current entity checkpoint |
| `driverClassName` | `driverclassname`, `driver_class_name` | no | JDBC driver override; normally inferred |

Do not provide both `year` and `yearMonth`. When none of `eTag`, `year`,
`yearMonth`, or `type` is supplied, the most recent complete set of dumps is
selected.

The current Discogs HTML index no longer exposes object ETags. For those
entries, the versioned path such as
`data/2026/discogs_20260701_releases.xml.gz` is used as the stable dump ID.
Each dump is paired with the same date's
`discogs_20260701_CHECKSUM.txt`. The displayed HTML file size is only an
estimate for progress reporting; existing and newly downloaded files are
accepted only after their SHA-256 value matches the manifest. One manifest is
fetched and cached per selected dump date.

If the directory index is unavailable, automatic selection falls back to the
monthly checksum manifests. It keeps the newest dump found for each entity and
looks further back only for unresolved entities, so one missing domain does not
roll the other three back. Access errors stop immediately instead of causing
repeated failing requests. File size is unknown on this fallback path, so
download progress is shown without a fixed total and integrity is still decided
by SHA-256.

The four selected dump checksums form one language-neutral manifest
fingerprint. A manifest that already completed successfully is skipped unless
`--force` is supplied. Failed imports remain retryable. Force never permits an
older dump by itself; downgrade requires the separate
`--allow-downgrade` option and is recorded in import history.

Java and Go importers use the same PostgreSQL entity advisory locks. Disjoint
entity sets may run concurrently, but a second process cannot update an entity
that is already being imported. The `discogs_import_checkpoint` database view
shows the last successfully applied dump date and provenance for each entity.

The application currently receives the database password as a process
argument. On a shared host, command-line arguments may be visible to other
users, so run it in an appropriately isolated environment and never commit
real credentials to scripts or configuration files.

## Contributing

Pull request titles and commit subjects must follow
[Conventional Commits](https://www.conventionalcommits.org/), for example:

```text
feat: add resumable dump downloads
fix(parser): handle an empty release identifier
docs: clarify PostgreSQL setup
```

Allowed types are `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`,
`refactor`, `revert`, `style`, and `test`. GitHub Actions rejects pull requests
and new commits that do not follow this format. Pull-request branches must also
not use the reserved `agent/`, `codex/`, or `claude/` prefixes.

## License

MIT. See [LICENSE](LICENSE). The `state303` attribution must be retained in
copies or substantial portions of the software.
