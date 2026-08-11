package io.dsub.discogs.batch.job.progress;

import io.dsub.discogs.batch.dump.EntityType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

public final class ImportProgressReporter {

  public static final Duration DEFAULT_REPORT_INTERVAL = Duration.ofSeconds(5);
  private static final double NANOSECONDS_PER_SECOND = 1_000_000_000.0;

  private final ImportProgressStore progressStore;
  private final EntityType entityType;
  private final long runId;
  private final boolean resumed;
  private final Clock clock;
  private final Duration reportInterval;
  private final ImportProgressSink sink;

  private Instant startedAt;
  private Instant lastReportedAt;
  private long initialCommittedItems;
  private boolean baselineSet;

  public ImportProgressReporter(
      ImportProgressStore progressStore,
      EntityType entityType,
      long runId,
      boolean resumed) {
    this(
        progressStore,
        entityType,
        runId,
        resumed,
        Clock.systemUTC(),
        DEFAULT_REPORT_INTERVAL,
        new Slf4jImportProgressSink());
  }

  ImportProgressReporter(
      ImportProgressStore progressStore,
      EntityType entityType,
      long runId,
      boolean resumed,
      Clock clock,
      Duration reportInterval,
      ImportProgressSink sink) {
    this.progressStore = progressStore;
    this.entityType = entityType;
    this.runId = runId;
    this.resumed = resumed;
    this.clock = clock;
    this.reportInterval = reportInterval;
    this.sink = sink;
    this.startedAt = clock.instant();
    this.lastReportedAt = startedAt;
  }

  public synchronized void start() {
    Instant now = clock.instant();
    startedAt = now;
    lastReportedAt = now;
    try {
      ImportProgressSnapshot snapshot = progressStore.getProgress(runId, entityType);
      initialCommittedItems = snapshot.committedItems();
      baselineSet = true;
      write(now, ImportProgressState.STARTED, snapshot);
    } catch (RuntimeException exception) {
      writeObservationError(now, exception);
    }
  }

  public synchronized void reportIfDue() {
    Instant now = clock.instant();
    if (Duration.between(lastReportedAt, now).compareTo(reportInterval) < 0) {
      return;
    }
    lastReportedAt = now;
    try {
      ImportProgressSnapshot snapshot = progressStore.getProgress(runId, entityType);
      setBaselineIfNeeded(snapshot);
      write(
          now,
          ImportProgressState.RUNNING,
          snapshot);
    } catch (RuntimeException exception) {
      writeObservationError(now, exception);
    }
  }

  public synchronized void finish(boolean success) {
    Instant now = clock.instant();
    try {
      ImportProgressSnapshot snapshot = progressStore.getProgress(runId, entityType);
      setBaselineIfNeeded(snapshot);
      write(
          now,
          success ? ImportProgressState.COMPLETED : ImportProgressState.FAILED,
          snapshot);
    } catch (RuntimeException exception) {
      writeObservationError(now, exception);
    }
  }

  private void write(
      Instant now, ImportProgressState state, ImportProgressSnapshot snapshot) {
    Duration elapsed = Duration.between(startedAt, now);
    double elapsedSeconds = elapsed.toNanos() / NANOSECONDS_PER_SECOND;
    double rowsPerSecond =
        elapsedSeconds > 0
            ? (snapshot.committedItems() - initialCommittedItems) / elapsedSeconds
            : 0;
    OptionalDouble committedPercent = committedPercent(snapshot);
    sink.write(
        new ImportProgressRecord(
            state,
            entityType,
            snapshot.committedItems(),
            committedPercent,
            rowsPerSecond,
            elapsed,
            resumed,
            initialCommittedItems,
            snapshot.lastCommittedProgressAt(),
            Optional.empty()));
  }

  private void setBaselineIfNeeded(ImportProgressSnapshot snapshot) {
    if (baselineSet) {
      return;
    }
    initialCommittedItems = snapshot.committedItems();
    baselineSet = true;
  }

  private OptionalDouble committedPercent(ImportProgressSnapshot snapshot) {
    if (snapshot.totalItems().isEmpty()) {
      return OptionalDouble.empty();
    }
    long totalItems = snapshot.totalItems().getAsLong();
    if (totalItems == 0) {
      return OptionalDouble.of(100);
    }
    return OptionalDouble.of(snapshot.committedItems() * 100.0 / totalItems);
  }

  private void writeObservationError(Instant now, RuntimeException exception) {
    sink.write(
        new ImportProgressRecord(
            ImportProgressState.OBSERVATION_ERROR,
            entityType,
            0,
            OptionalDouble.empty(),
            0,
            Duration.between(startedAt, now),
            resumed,
            initialCommittedItems,
            Optional.empty(),
            Optional.ofNullable(exception.getMessage())));
  }
}
