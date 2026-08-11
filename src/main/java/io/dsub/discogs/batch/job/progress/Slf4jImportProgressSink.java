package io.dsub.discogs.batch.job.progress;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Slf4jImportProgressSink implements ImportProgressSink {

  private static final String EVENT_NAME = "import_progress";
  private static final String UNAVAILABLE = "unavailable";
  private static final String EMPTY = "";
  private static final String TWO_DECIMAL_FORMAT = "%.2f";
  private static final String THREE_DECIMAL_FORMAT = "%.3f";
  private static final double NANOSECONDS_PER_SECOND = 1_000_000_000.0;
  private static final String LOG_TEMPLATE =
      "event={} state={} entity={} committed_items={} committed_percent={} "
          + "rows_per_second={} elapsed_seconds={} resumed={} "
          + "initial_committed_items={} last_committed_progress_at={} observation_error={}";

  @Override
  public void write(ImportProgressRecord record) {
    String percent =
        record.committedPercent().isPresent()
            ? String.format(
                Locale.ROOT, TWO_DECIMAL_FORMAT, record.committedPercent().getAsDouble())
            : UNAVAILABLE;
    String lastProgress =
        record.lastCommittedProgressAt().map(timestamp -> timestamp.toString()).orElse(UNAVAILABLE);
    String observationError = record.observationError().orElse(EMPTY);
    String state = record.state().name().toLowerCase(Locale.ROOT);
    String rowsPerSecond =
        String.format(Locale.ROOT, TWO_DECIMAL_FORMAT, record.rowsPerSecond());
    String elapsedSeconds = String.format(Locale.ROOT, THREE_DECIMAL_FORMAT, seconds(record));
    if (record.state() == ImportProgressState.OBSERVATION_ERROR) {
      log.warn(
          LOG_TEMPLATE,
          EVENT_NAME,
          state,
          record.entityType().toString(),
          record.committedItems(),
          percent,
          rowsPerSecond,
          elapsedSeconds,
          record.resumed(),
          record.initialCommittedItems(),
          lastProgress,
          observationError);
    } else {
      log.info(
          LOG_TEMPLATE,
          EVENT_NAME,
          state,
          record.entityType().toString(),
          record.committedItems(),
          percent,
          rowsPerSecond,
          elapsedSeconds,
          record.resumed(),
          record.initialCommittedItems(),
          lastProgress,
          observationError);
    }
  }

  private double seconds(ImportProgressRecord record) {
    return record.elapsed().toNanos() / NANOSECONDS_PER_SECOND;
  }
}
