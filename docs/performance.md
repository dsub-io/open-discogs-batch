# Performance measurements

These are bounded measurements of named changes, not forecasts for a full dump
or different hardware. They also do not approve a production import: both
batch implementations still require release and cross-language validation
against canonical `open-discogs-model` v0.3.1.

## Results at a glance

| Change | Measured outcome | Scope |
| --- | --- | --- |
| Reference ID cache | 22.4× lower median time; 60.7% lower maximum RSS | 1,000,000 positive IDs |
| Durable import contract | +4.5% p50 and 4.3% lower median throughput | 24-record forced-idempotency fixture |
| Release Master lock candidates | 161 s observed maximum to 158.144 ms; about 1,018× faster | One real 5,000-Release production chunk |
| Format quantity parser | 89.3–95.0% lower Go median time; Java not timed | Typical and 52-digit values |

Do not compare the two rows directly. Their harnesses and measured paths are
different.

## Reference ID cache

On an Apple M2 Pro with Java 21, the previous skip-list cache and two inversion
passes were compared with the segmented bit set using 1,000,000 positive IDs
across three fresh-process runs.

| Metric | Before | After | Change |
| --- | ---: | ---: | ---: |
| Median elapsed | 648.549 ms | 28.997 ms | 22.4× faster |
| Median maximum RSS | 163.9 MB | 64.47 MB | 60.7% lower |
| Bit-set words | — | 128 KiB | — |

## Durable import cost

The forced-idempotency path was measured against v1.0.0 on the same Apple M2
Pro with Java 21, PostgreSQL 18.4 Alpine on tmpfs, four 3-record fixtures,
`chunk-size=1000`, one worker, two imports per sample, two warm-ups, and 20
fresh samples.

| Metric | Before | After | Change |
| --- | ---: | ---: | ---: |
| p50/p95/p99 for both imports | 734/845/854 ms | 767/916/980 ms | +4.5%/+8.4%/+14.8% |
| Median throughput | 32.7 records/s | 31.3 records/s | 4.3% lower |

The added fixed cost covers active-run fencing, exact source-chunk ledger
commits, coverage validation, and stale-relation reconciliation. This 24-record
fixture exaggerates fixed transaction cost and cannot represent a full import.
RSS and allocation deltas are not reported because the isolated process
includes Gradle and Testcontainers while the fixture is too small to represent
production memory.

## Release Master lock candidates

The first production retry exposed a full-table scan in the shared Release
Master lock query. Combining target IDs, current main-release IDs, and an
`EXISTS` branch with `OR` made PostgreSQL scan all 2,579,897 Master rows for
each of four workers. The running query reached 161 seconds; each backend used
1.20--1.29 GiB PSS, three workers waited in a transaction-lock chain, and the
PostgreSQL cgroup reached 12.7 GiB.

The production host had 8 vCPUs, 15.62 GiB RAM, rotational PostgreSQL storage,
PostgreSQL 17.7, `chunk-size=5000`, and `max-workers=4`. Go and Java now use the
same query shape: union candidate IDs through indexed
`master.id`, `master.main_release_id`, and `release_item.id` lookups, join those
IDs to `master`, and lock the resulting rows in ascending order. A real
5,000-Release production chunk covering IDs 840001--845000 produced 2,275
candidate Masters and completed `EXPLAIN (ANALYZE, BUFFERS, WAL)` in 158.144
ms. The plan used indexed primary-key lookups, with 1.270 ms planning time,
6,832 shared buffer hits, 2,268 shared buffer reads, 7.558 ms read time, and no
full Master scan. This is about 99.90% lower execution latency, or 1,018×
faster than the observed 161-second query.

The before query was stopped to protect the shared database, so p50/p95/p99 and
a controlled before/after RSS comparison are unavailable. The after query ran
once in a rolled-back transaction after planner statistics and production
PostgreSQL limits were updated. Full-import throughput and steady-state RSS
remain to be measured during the next import.

## Release format quantity parser

The release dump contains 19,810,850 format rows, and release `6662697` has a
quantity beyond signed 32-bit storage. Both importers replaced repeated
arbitrary-precision parsing with the same digit scan, zero trimming, and
lexical int32 boundary.

The checked-in Go benchmark ran on an Apple M2 Pro with five samples per path.
For `0002`, median time fell from 152.5 ns to 16.36 ns (89.3%, 9.3×), bytes
from 48 B to 4 B, and allocations from four to one. For the 52-digit dump
value, median time fell from 587.1 ns to 29.58 ns (95.0%, 19.8×), while 280 B
and six allocations fell to zero.

Those numbers isolate the Go parser. They do not measure the JVM, complete
format transformation, database throughput, or a full dump. The Java path has
the same bounded algorithm but no numerical JVM claim is made.

## Validation and reproduction limits

The exact measurement harnesses are not checked into this repository:

- The reference-cache table used three fresh JVM processes and external maximum
  RSS collection. The command below does not recreate that harness.
- The durable-import table compared `v1.0.0` with the durability implementation
  using two warm-ups and 20 fresh-process samples. The current checkout contains
  neither the historical binary orchestration nor percentile aggregation.

The available Gradle command validates the current forced-idempotency path
once; it does not reproduce either before/after table:

```shell
./gradlew cleanE2eTest e2eTest \
  --tests 'io.dsub.discogs.batch.job.PostgreSQLDiscogsJobE2ETest.forcedRefreshPreservesTheCanonicalBusinessState' \
  --quiet
```

The figures are historical records. A numerical rerun must restore both exact
revisions and the external fresh-process/RSS harness, then use the stated Apple
M2 Pro, Java 21, PostgreSQL 18.4 tmpfs, warm-up, and sample counts. The tiny
fixtures do not measure production-sized imports and must not be extrapolated
to 200 million rows.
