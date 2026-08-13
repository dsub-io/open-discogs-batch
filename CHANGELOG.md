# Changelog

## [1.2.3](https://github.com/dsub-io/open-discogs-batch/compare/v1.2.2...v1.2.3) (2026-08-13)


### Bug Fixes

* preserve canonical dump data and bound full imports ([#48](https://github.com/dsub-io/open-discogs-batch/issues/48)) ([30bde02](https://github.com/dsub-io/open-discogs-batch/commit/30bde020b0975051c831131ba8da4aa560bbd43e))

### Data Correctness and Recovery

* consume canonical model `0.3.1`, preserve distinct rows that collide under
  legacy 32-bit hashes, collapse exact duplicates, retain release format
  quantity text when it exceeds the integer range, and use the same relation
  identities and migration history as the Go batch
* keep chunk commits, import ownership, stale-relation reconciliation, and
  backlink updates restart-safe across process termination and PostgreSQL
  interruption; a production resume skipped `34,489,698` already committed
  roots instead of rewriting them

### Measured Performance and Validation

* on the same production database and unchanged dump, backlink reconciliation
  fell from `61.027 s` to `8.584 s` (`85.9%` lower) with `0` changed rows and
  `0` WAL bytes for the unchanged pass
* seed fresh master backlinks during the initial insert instead of issuing the
  former second update pass; the observed legacy pass updated `2,579,769` rows
  in `289.960 s` and generated `4,379,751,749` WAL bytes, while a fresh
  production-scale measurement of the new bootstrap path remains pending
* pass the clean Gradle build, `100.0%` line and branch coverage gates,
  PostgreSQL interruption/resume E2E, and cross-language state checks with no
  residual test container, network, or volume

### Distribution

* publish the executable JAR and SHA-256 checksum plus non-root
  `linux/amd64` and `linux/arm64` GHCR images through the protected release
  workflow

## [1.2.2](https://github.com/dsub-io/open-discogs-batch/compare/v1.2.1...v1.2.2) (2026-08-12)


### Bug Fixes

* harden canonical dump recovery ([d0a3ee2](https://github.com/dsub-io/open-discogs-batch/commit/d0a3ee21c5b22a2443bf187586f9d618770d3867))
* make canonical dump recovery atomic ([#43](https://github.com/dsub-io/open-discogs-batch/issues/43)) ([a3101c8](https://github.com/dsub-io/open-discogs-batch/commit/a3101c8d8ed5769b27ae384f2c27629e1e63aaec))
* preserve release label catalog identities ([a6f72e7](https://github.com/dsub-io/open-discogs-batch/commit/a6f72e7514785c4b2e51f77c240a7e6e8531491b))
* restore 1.2.1 release state ([b687dec](https://github.com/dsub-io/open-discogs-batch/commit/b687decefff44659314b5d2fe69002d06c37dc04))
* restore 1.2.1 release state ([#46](https://github.com/dsub-io/open-discogs-batch/issues/46)) ([3c2d4df](https://github.com/dsub-io/open-discogs-batch/commit/3c2d4dfe280b2d954d17b65d187418c3e1b6efb9))

## [1.2.1](https://github.com/dsub-io/open-discogs-batch/compare/v1.2.0...v1.2.1) (2026-08-12)


### Bug Fixes

* keep terminal progress output readable ([#41](https://github.com/dsub-io/open-discogs-batch/issues/41)) ([b00c581](https://github.com/dsub-io/open-discogs-batch/commit/b00c5812a0fb6b3004bc861857e01e3607f22a4a))
* make an interactive invocation display only the progress bar by suppressing
  periodic `event=import_progress` console records while the bar is active
* suppress carriage-return progress bars in redirected output, pipelines,
  containers, and Kubernetes while retaining structured progress logs

### Measured Output and Validation

* interactive structured progress falls from up to `0.2` records per second per
  active entity to `0` (`100%` reduction)
* non-interactive bar output falls from up to `10` renders per second to `0`
  (`100%` reduction), while interactive refresh remains unchanged
* the clean Gradle build and PostgreSQL E2E passed with `4,010/4,010` lines and
  `1,309/1,309` branches covered and no residual test container, network, or
  volume

## [1.2.0](https://github.com/dsub-io/open-discogs-batch/compare/v1.1.1...v1.2.0) (2026-08-11)


### Features

* add operator-selected PostgreSQL schemas through `--database-schema` and `OPEN_DISCOGS_BATCH_DATABASE_SCHEMA` ([9756f20](https://github.com/dsub-io/open-discogs-batch/commit/9756f20c77f372477aad63c432999fe038584e9b))
* create a missing selected schema and keep Liquibase, Spring Batch metadata, and imported catalog tables inside it
* retain `public` as the compatibility default while warning on every startup and documenting database and role prerequisites

## [1.1.1](https://github.com/dsub-io/open-discogs-batch/compare/v1.1.0...v1.1.1) (2026-08-11)


### Bug Fixes

* generate the SHA-256 manifest from the copied release JAR so its entry uses
  the portable `open-discogs-batch-<version>.jar` filename instead of the
  repository-only `build/libs/...` path ([#37](https://github.com/dsub-io/open-discogs-batch/pull/37))
* verify the generated checksum before upload, causing the release workflow to
  fail before publishing inconsistent JAR and checksum assets

### Validation

* pass Actionlint and the release-equivalent Gradle build, PostgreSQL E2E, and
  executable JAR version check for `1.1.1`
* verify `sha256sum --check` succeeds with the portable filename, maintain zero
  missed JaCoCo instructions and lines, and leave no test Docker resources

## [1.1.0](https://github.com/dsub-io/open-discogs-batch/compare/v1.0.2...v1.1.0) (2026-08-11)


### Features

* label exact local compressed-byte percentage, throughput, elapsed time, and
  source ETA as `SOURCE READ`, separate from database commit progress ([#34](https://github.com/dsub-io/open-discogs-batch/pull/34))
* emit structured `import_progress` logs with exact durable committed items,
  resume baseline, current-run rows per second, last commit time, and explicit
  started, running, completed, failed, and non-fatal observation-error states

### Scale and Validation

* avoid a full XML pre-count pass; each emitted sample performs one primary-key
  summary read, with running observations bounded to once every five seconds
  (`0.2 reads/second` per active entity) plus start and finish reads
* pass the clean Gradle build with `0` missed JaCoCo instructions, branches,
  lines, complexity, methods, and classes; the PostgreSQL E2E verifies failure,
  partial commit, resume baseline, and completion with no residual Docker
  resources
* the first production-sized dump remains intentionally deferred, so no
  200-million-row throughput, heap, or completion-time claim is inferred from
  fixtures

## [1.0.2](https://github.com/dsub-io/open-discogs-batch/compare/v1.0.1...v1.0.2) (2026-08-11)


### Performance Improvements

* update the Maven Central model to `0.2.2` and apply canonical `V007` API query indexes through Liquibase ([e581e38](https://github.com/dsub-io/open-discogs-batch/commit/e581e38f5037a80d48adafb13f3acb5ba95b39df))

### Measured Impact

* on the same warm-cache PostgreSQL 18.4 synthetic dataset, deep release pagination p95 fell from `183.106 ms` to `0.038 ms` (`99.979%` lower, `4,818.6x` faster) with keyset pagination
* indexed title-contains search p95 fell from `194.535 ms` to `0.136 ms` (`99.930%` lower, `1,430.4x` faster), and reverse artist-relation lookup p95 fell from `17.309 ms` to `0.061 ms` (`99.648%` lower, `283.8x` faster)
* the synthetic database grew from `314,308,287` to `486,389,439` bytes (`+164.1 MiB`, `+54.7%`); full 200M+ import duration, production index size, cold I/O, and concurrent throughput remain pre-production measurements

### Validation and Distribution

* apply V001 through V007 plus the Spring Batch schema against actual PostgreSQL and pass durability/idempotency E2E
* maintain `100.0%` JaCoCo coverage for instructions, branches, lines, complexity, methods, and classes, with zero residual test containers, networks, or volumes
* publish the executable JAR and checksum plus non-root `linux/amd64` and `linux/arm64` GHCR images with post-publish architecture verification

## [1.0.1](https://github.com/dsub-io/open-discogs-batch/compare/v1.0.0...v1.0.1) (2026-08-10)


### Durability and Idempotency

* persist immutable import manifests, stable run identity, per-entity progress, and exact committed source-chunk ledgers
* resume only compatible failed runs, skip successful manifests only while their entity checkpoints remain current, and make forced runs start fresh
* commit relation convergence and its progress ledger atomically, fence canonical writes to the active run, and lock selected entities with their Artist, Label, and Master dependencies
* propagate nested step failures, require complete entity coverage before success, and preserve durable database success when optional file cleanup fails

### Measured Impact

* on the same Apple M2 Pro, Java 21, PostgreSQL 18.4 Alpine tmpfs, four 3-record fixtures, chunk size 1,000, one worker, two warm-ups, and 20 samples, forced two-import p50/p95/p99 changed from `734/845/854 ms` to `767/916/980 ms` (`+4.5%/+8.4%/+14.8%`)
* median throughput changed from `32.7` to `31.3 records/s` (`-4.3%`); this is the measured cost of checkpoint validation, exact ledgers, and active-run fencing on a tiny fixture, not a 200M-row throughput estimate
* whole-suite line coverage increased from `93.33%` to `100.00%` (`+6.67` percentage points), and branch coverage increased from `78.50%` to `100.00%` (`+21.50` percentage points)

### Validation and Distribution

* verify restart, failure, cleanup, physical idempotency, and relation convergence against PostgreSQL with no residual test containers, networks, or volumes
* publish the versioned executable JAR, SHA-256 checksum, and non-root `linux/amd64` and `linux/arm64` GHCR images through the protected release workflow

## [1.0.0](https://github.com/dsub-io/open-discogs-batch/compare/v0.1.8...v1.0.0) (2026-08-09)


### ⚠ BREAKING CHANGES

* replace legacy Java runtime options with the shared OpenDiscogs Batch contract. Add exact max-workers control, bounded chunk handoff, segmented ID caches, streamed dependency IDs, bounded relation writes, and multi-architecture GitHub/GHCR release publication.

### Features

* unify runtime options and optimize large imports ([#24](https://github.com/dsub-io/open-discogs-batch/issues/24)) ([8673730](https://github.com/dsub-io/open-discogs-batch/commit/8673730146501d77c15470586bbf24597fde3229))

### Performance Improvements

* replace skip-list ID caches and inversion passes with segmented bit sets; in the same 1,000,000-ID benchmark, median elapsed time fell from 648.549 ms to 28.997 ms (`22.4x` faster) and median maximum RSS fell from 163.9 MB to 64.47 MB (`60.7%` lower)
* remove the executor waiting queue, stream dependency IDs with a 10,000-row cursor, and bound expanded relation writes to `chunk-size`

### Distribution

* publish a versioned executable JAR and SHA-256 checksum to GitHub Releases
* publish non-root `linux/amd64` and `linux/arm64` images to `ghcr.io/dsub-io/open-discogs-batch`, with a post-publish architecture check
