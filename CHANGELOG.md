# Changelog

## [2.0.0](https://github.com/dsub-io/open-discogs-batch/compare/v1.0.1...v2.0.0) (2026-08-10)


### ⚠ BREAKING CHANGES

* replace legacy Java runtime options with the shared OpenDiscogs Batch contract. Add exact max-workers control, bounded chunk handoff, segmented ID caches, streamed dependency IDs, bounded relation writes, and multi-architecture GitHub/GHCR release publication.

### Features

* unify runtime options and optimize large imports ([#24](https://github.com/dsub-io/open-discogs-batch/issues/24)) ([8673730](https://github.com/dsub-io/open-discogs-batch/commit/8673730146501d77c15470586bbf24597fde3229))


### Bug Fixes

* close batch correctness and coverage gaps ([eeeae1a](https://github.com/dsub-io/open-discogs-batch/commit/eeeae1a747fb0ee47fd337848bb6d451ca152f7f))
* make snapshot imports durable and convergent ([7d93c07](https://github.com/dsub-io/open-discogs-batch/commit/7d93c077f1c51cbcf16cb38beceb220c2432e5ad))
* make snapshot imports durable and convergent ([#26](https://github.com/dsub-io/open-discogs-batch/issues/26)) ([ec2f6a0](https://github.com/dsub-io/open-discogs-batch/commit/ec2f6a0476a6af3bcf25a77e62fbe423f33f1434))

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
