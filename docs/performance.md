# Performance measurements

These measurements isolate named changes. They are not estimates of full-dump
throughput on other hardware.

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

The forced idempotency path was measured against v1.0.0 on the same Apple M2
Pro with Java 21, PostgreSQL 18.4 Alpine on tmpfs, four 3-record fixtures,
`chunk-size=1000`, one worker, two imports per sample, two warm-ups, and 20 fresh
samples.

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

## Reproduction

```shell
./gradlew cleanTest test \
  --tests 'io.dsub.discogs.batch.job.PostgreSQLDiscogsJobIntegrationTest.whenSameDumpIsForcedTwice__BusinessRowsRemainIdentical' \
  --quiet
```
