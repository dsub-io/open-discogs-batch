# Import safety and recovery

This document defines the operational boundary behind the shorter README.

## Dump discovery

Artist, label, master, and release select their newest dumps independently
unless `--dump-month` requests an exact month. Every run records dump dates,
SHA-256 checksums, source paths, sizes, and stable identifiers in one immutable
manifest. A missing domain in one month does not roll other domains back.

Discogs paths such as `data/2026/discogs_20260701_releases.xml.gz` are paired
with the checksum manifest from the same date.

## Admission and locking

A successful manifest is skipped unless `--force` requests a fresh run.
PostgreSQL advisory locks cover selected entities and their references:

- Artist locks Artist.
- Label locks Label.
- Master locks Artist and Master.
- Release locks Artist, Label, Master, and Release because it also updates
  `master.main_release_id`.

Independent sets such as Artist and Label may run together. Overlapping Go and
Java imports cannot write concurrently.

## Commit and convergence boundary

Canonical relation changes and the source-chunk ledger entry share one
PostgreSQL transaction. A retry reads the stream from the beginning, skips
exact committed relation chunks, and safely reruns core rows and post-relation
work. Missing relations are deleted, changed values are updated, and unchanged
rows retain their surrogate IDs. Root rows are upserted; roots absent from the
complete dump are not currently deleted.

An entity completes only after end-of-stream validation confirms its exact
coverage and totals. A run becomes successful only after every selected entity
completes.

The schema still uses signed 32-bit Java hashes for several relation identities.
Distinct values can collide within one root. Migration to collision-resistant
identity is tracked in
[`open-discogs-model#43`](https://github.com/dsub-io/open-discogs-model/issues/43).

## Interruption and resume

A failed or abandoned run resumes only when manifest, processor version,
entity set, dump identities, and chunk size match. `--force` always starts from
zero. Failed imports retain their files.

Cleanup runs only after a successful database import. Cleanup failure is
reported without changing committed import success, and the next invocation
retries cleanup through the successful-manifest skip path.

## Snapshot visibility

Atomicity is per chunk, not per monthly snapshot. Permanent full-dump staging
is intentionally avoided because it would duplicate a catalog exceeding 200
million records. Readers may observe committed chunks during an import.

Deployments requiring an all-at-once switch must import into a separate
versioned database or replica and promote it after validation.
