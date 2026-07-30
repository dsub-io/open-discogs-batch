# OpenDiscogs Batch

OpenDiscogs Batch imports the public monthly data dumps from
[data.discogs.com](https://data.discogs.com) into PostgreSQL. It uses Spring
Batch for orchestration, Liquibase for schema setup, and jOOQ for database
writes.

This is an independent DSUB project. It is not affiliated with or endorsed by
Discogs. The Discogs name is used only to identify the public data source.

## What it does

- Downloads a selected dump, or resolves the most recent available dump.
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

Run the complete test suite:

```bash
./gradlew clean test
```

Some integration tests start PostgreSQL through Testcontainers, and some
exercise the live dump index at `data.discogs.com`. They therefore require
Docker and external network access.

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
| `eTag` | `e` | no | Exact dump ETag; overrides year, month, and type selection |
| `chunkSize` | `chunk`, `c` | no | Spring Batch chunk size; defaults to `500` |
| `coreCount` | `core` | no | Worker count; defaults to 80% of physical cores |
| `mount` | `m` | no | Keep downloaded dump files |
| `strict` | `s` | no | Do not add entity dependencies |
| `driverClassName` | `driverclassname`, `driver_class_name` | no | JDBC driver override; normally inferred |

Do not provide both `year` and `yearMonth`. When none of `eTag`, `year`,
`yearMonth`, or `type` is supplied, the most recent complete set of dumps is
selected.

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
