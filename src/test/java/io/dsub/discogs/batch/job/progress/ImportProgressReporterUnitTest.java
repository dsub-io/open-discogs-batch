package io.dsub.discogs.batch.job.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.EntityType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ImportProgressReporterUnitTest {

  private static final Instant START = Instant.parse("2026-08-11T12:00:00Z");

  @Test
  void reportsStartedRunningAndCompletedDurableProgress() {
    ImportProgressStore store = mock(ImportProgressStore.class);
    Instant lastProgress = START.minusSeconds(30);
    when(store.getProgress(7L, EntityType.RELEASE))
        .thenReturn(
            snapshot(40, OptionalLong.empty(), Optional.of(lastProgress)),
            snapshot(60, OptionalLong.empty(), Optional.empty()),
            snapshot(100, OptionalLong.of(100), Optional.empty()));
    MutableClock clock = new MutableClock(START);
    List<ImportProgressRecord> records = new ArrayList<>();
    ImportProgressReporter reporter =
        new ImportProgressReporter(
            store,
            EntityType.RELEASE,
            7L,
            true,
            clock,
            ImportProgressReporter.DEFAULT_REPORT_INTERVAL,
            records::add);

    reporter.start();
    reporter.reportIfDue();
    clock.advance(ImportProgressReporter.DEFAULT_REPORT_INTERVAL);
    reporter.reportIfDue();
    clock.advance(ImportProgressReporter.DEFAULT_REPORT_INTERVAL);
    reporter.finish(true);

    assertThat(records).hasSize(3);
    assertThat(records.get(0).state()).isEqualTo(ImportProgressState.STARTED);
    assertThat(records.get(0).initialCommittedItems()).isEqualTo(40);
    assertThat(records.get(0).lastCommittedProgressAt()).contains(lastProgress);
    assertThat(records.get(0).committedPercent()).isEmpty();
    assertThat(records.get(1).state()).isEqualTo(ImportProgressState.RUNNING);
    assertThat(records.get(1).rowsPerSecond()).isEqualTo(4);
    assertThat(records.get(2).state()).isEqualTo(ImportProgressState.COMPLETED);
    assertThat(records.get(2).committedPercent()).hasValue(100);
    assertThat(records.get(2).rowsPerSecond()).isEqualTo(6);
    verify(store, times(3)).getProgress(7L, EntityType.RELEASE);
  }

  @Test
  void reportsFailuresZeroTotalsAndObservationErrorsWithoutThrowing() {
    ImportProgressStore store = mock(ImportProgressStore.class);
    RuntimeException withMessage = new IllegalStateException("fixture");
    RuntimeException withoutMessage = new IllegalStateException();
    when(store.getProgress(8L, EntityType.ARTIST))
        .thenThrow(withMessage)
        .thenThrow(withoutMessage)
        .thenThrow(withMessage)
        .thenReturn(snapshot(0, OptionalLong.of(0), Optional.empty()));
    MutableClock clock = new MutableClock(START);
    List<ImportProgressRecord> records = new ArrayList<>();
    ImportProgressReporter reporter =
        new ImportProgressReporter(
            store,
            EntityType.ARTIST,
            8L,
            false,
            clock,
            ImportProgressReporter.DEFAULT_REPORT_INTERVAL,
            records::add);

    reporter.start();
    clock.advance(ImportProgressReporter.DEFAULT_REPORT_INTERVAL);
    reporter.reportIfDue();
    reporter.finish(true);
    reporter.finish(false);

    assertThat(records).hasSize(4);
    assertThat(records.subList(0, 3))
        .allSatisfy(
            record -> assertThat(record.state())
                .isEqualTo(ImportProgressState.OBSERVATION_ERROR));
    assertThat(records.get(0).observationError()).contains("fixture");
    assertThat(records.get(1).observationError()).isEmpty();
    assertThat(records.get(3).state()).isEqualTo(ImportProgressState.FAILED);
    assertThat(records.get(3).committedPercent()).hasValue(100);
    assertThat(records.get(3).initialCommittedItems()).isZero();
    assertThat(records.get(3).rowsPerSecond()).isZero();
  }

  @Test
  void establishesRateBaselineAfterStartObservationFailure() {
    ImportProgressStore store = mock(ImportProgressStore.class);
    when(store.getProgress(10L, EntityType.MASTER))
        .thenThrow(new IllegalStateException("fixture"))
        .thenReturn(snapshot(25, OptionalLong.of(25), Optional.empty()));
    MutableClock clock = new MutableClock(START);
    List<ImportProgressRecord> records = new ArrayList<>();
    ImportProgressReporter reporter =
        new ImportProgressReporter(
            store,
            EntityType.MASTER,
            10L,
            false,
            clock,
            ImportProgressReporter.DEFAULT_REPORT_INTERVAL,
            records::add);

    reporter.start();
    clock.advance(ImportProgressReporter.DEFAULT_REPORT_INTERVAL);
    reporter.reportIfDue();

    assertThat(records).hasSize(2);
    assertThat(records.get(1).state()).isEqualTo(ImportProgressState.RUNNING);
    assertThat(records.get(1).initialCommittedItems()).isEqualTo(25);
    assertThat(records.get(1).rowsPerSecond()).isZero();
  }

  @Test
  void publicReporterUsesProductionDefaults() {
    ImportProgressStore store = mock(ImportProgressStore.class);
    when(store.getProgress(9L, EntityType.LABEL))
        .thenReturn(snapshot(0, OptionalLong.empty(), Optional.empty()));
    ImportProgressReporter reporter =
        new ImportProgressReporter(store, EntityType.LABEL, 9L, false);

    reporter.start();
  }

  @Test
  void slf4jSinkWritesAvailableAndUnavailableValues() {
    Slf4jImportProgressSink sink = new Slf4jImportProgressSink();
    sink.write(
        new ImportProgressRecord(
            ImportProgressState.RUNNING,
            EntityType.MASTER,
            5,
            OptionalDouble.of(50),
            2.5,
            Duration.ofMillis(1500),
            true,
            1,
            Optional.of(START),
            Optional.empty()));
    sink.write(
        new ImportProgressRecord(
            ImportProgressState.OBSERVATION_ERROR,
            EntityType.MASTER,
            0,
            OptionalDouble.empty(),
            0,
            Duration.ZERO,
            false,
            0,
            Optional.empty(),
            Optional.of("fixture")));
  }

  private ImportProgressSnapshot snapshot(
      long committedItems,
      OptionalLong totalItems,
      Optional<Instant> lastProgress) {
    return new ImportProgressSnapshot(committedItems, totalItems, lastProgress);
  }

  private static final class MutableClock extends Clock {

    private Instant current;

    private MutableClock(Instant current) {
      this.current = current;
    }

    private void advance(Duration duration) {
      current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(current, zone);
    }

    @Override
    public Instant instant() {
      return current;
    }
  }
}
