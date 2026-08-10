# Changelog

## [1.0.1](https://github.com/dsub-io/open-discogs-batch/compare/v1.0.0...v1.0.1) (2026-08-10)


### Bug Fixes

* close batch correctness and coverage gaps ([eeeae1a](https://github.com/dsub-io/open-discogs-batch/commit/eeeae1a747fb0ee47fd337848bb6d451ca152f7f))
* make snapshot imports durable and convergent ([7d93c07](https://github.com/dsub-io/open-discogs-batch/commit/7d93c077f1c51cbcf16cb38beceb220c2432e5ad))
* make snapshot imports durable and convergent ([#26](https://github.com/dsub-io/open-discogs-batch/issues/26)) ([ec2f6a0](https://github.com/dsub-io/open-discogs-batch/commit/ec2f6a0476a6af3bcf25a77e62fbe423f33f1434))

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
