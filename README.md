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

Files are selected as a coherent dump-date set. A full import requires all four
types from the same date. A targeted import only requires the selected type and
its dependencies, so a missing label dump does not prevent an artist-only
import. If the required set is incomplete for a month, automatic selection
moves to the previous month.

The current schema is documented in the
[OpenDiscogs ERD](https://dbdocs.io/state303/OpenDiscogs).

## Requirements

- JDK 16
- A reachable PostgreSQL database
- Network access to `data.discogs.com`
- Docker for the Testcontainers-based integration tests

Dependencies resolve from Maven Central. The generated schema library is
published as:

```text
io.dsub.opendiscogs:open-discogs-jooq:0.0.4
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
Docker. Tests that access `data.discogs.com` are kept outside the deterministic
suite and can be run explicitly:

```bash
./gradlew e2eTest
```

The same E2E suite runs weekly, can be started manually, and runs on a pull
request when the `e2e` label is present. It is intentionally not a required
pull-request check because it depends on an external service.

Executable test classes use one of three suffixes:

- `*UnitTest` for isolated tests.
- `*IntegrationTest` for deterministic component and Testcontainers tests.
- `*E2ETest` for opt-in end-to-end checks against external services.

The build rejects other executable test class names. Pull requests must pass
the deterministic suite and maintain at least 85% line coverage and 40% branch
coverage.

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
and new commits that do not follow this format.
