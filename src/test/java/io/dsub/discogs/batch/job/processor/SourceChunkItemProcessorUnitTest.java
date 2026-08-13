package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.CompletedChunkInventory;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

class SourceChunkItemProcessorUnitTest {

  @Test
  void preservesRangeWhileFilteringNullDelegateResults() throws Exception {
    Clock clock = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
    List<LocalDateTime> observedTimes = new ArrayList<>();
    SourceChunkItemProcessor<Integer, String> processor =
        new SourceChunkItemProcessor<>(
            (value, observedAt) -> {
              observedTimes.add(observedAt);
              return value == 2 ? null : "value-" + value;
            },
            clock);
    ChunkRange range = new ChunkRange(0, 0, 3);

    ProcessedChunk<String> processed =
        processor.process(new SourceChunk<>(range, List.of(1, 2, 3)));

    assertThat(processed.range()).isEqualTo(range);
    assertThat(processed.values()).containsExactly("value-1", "value-3");
    assertThat(observedTimes)
        .containsOnly(LocalDateTime.of(2026, 8, 1, 0, 0))
        .hasSize(3);
  }

  @Test
  void resumeFilterSkipsCompletedChunkBeforeCallingTheDelegate() throws Exception {
    ChunkRange completedRange = new ChunkRange(2, 10, 5);
    AtomicInteger calls = new AtomicInteger();
    ResumeAwareSourceChunkItemProcessor<Integer, String> processor =
        new ResumeAwareSourceChunkItemProcessor<>(
            source -> {
              calls.incrementAndGet();
              return new ProcessedChunk<>(source.range(), List.of("processed"));
            },
            new CompletedChunkInventory(EntityType.ARTIST, Map.of(2L, completedRange)));

    assertThat(processor.process(new SourceChunk<>(completedRange, List.of(1, 2, 3, 4, 5))))
        .isNull();
    assertThat(calls).hasValue(0);

    ChunkRange pendingRange = new ChunkRange(3, 15, 5);
    assertThat(processor.process(new SourceChunk<>(pendingRange, List.of(1, 2, 3, 4, 5))).values())
        .containsExactly("processed");
    assertThat(calls).hasValue(1);
  }

  @Test
  void resumeFilterRejectsAChangedCompletedRange() {
    ResumeAwareSourceChunkItemProcessor<Integer, String> processor =
        new ResumeAwareSourceChunkItemProcessor<>(
            source -> new ProcessedChunk<>(source.range(), List.of()),
            new CompletedChunkInventory(
                EntityType.RELEASE,
                Map.of(1L, new ChunkRange(1, 5, 5))));

    assertThatThrownBy(
            () -> processor.process(
                new SourceChunk<>(new ChunkRange(1, 6, 5), List.of(1, 2, 3, 4, 5))))
        .hasMessageContaining("does not match the source range");
    assertThatThrownBy(
            () -> processor.process(
                new SourceChunk<>(new ChunkRange(1, 5, 4), List.of(1, 2, 3, 4))))
        .hasMessageContaining("does not match the source range");
  }

  @Test
  void executesPreparationThroughTheConfiguredBoundaryAndPreservesCheckedFailures() {
    AtomicInteger boundaryCalls = new AtomicInteger();
    TransactionOperations boundary =
        new TransactionOperations() {
          @Override
          public <T> T execute(TransactionCallback<T> callback) {
            boundaryCalls.incrementAndGet();
            return callback.doInTransaction(null);
          }
        };
    SourceChunkItemProcessor<Integer, String> successful =
        new SourceChunkItemProcessor<>((value, observedAt) -> value.toString(), boundary);

    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    successful.process(
                        new SourceChunk<>(new ChunkRange(0, 0, 1), List.of(1)))))
        .isNull();
    assertThat(boundaryCalls).hasValue(1);

    Exception expected = new Exception("checked fixture");
    SourceChunkItemProcessor<Integer, String> failing =
        new SourceChunkItemProcessor<>(
            (value, observedAt) -> {
              throw expected;
            },
            boundary);
    assertThatThrownBy(
            () ->
                failing.process(
                    new SourceChunk<>(new ChunkRange(1, 1, 1), List.of(2))))
        .isSameAs(expected);
  }

  @Test
  void defaultBoundaryPreservesRuntimeFailures() {
    IllegalStateException expected = new IllegalStateException("runtime fixture");
    SourceChunkItemProcessor<Integer, String> processor =
        new SourceChunkItemProcessor<>(
            (value, observedAt) -> {
              throw expected;
            });

    assertThatThrownBy(
            () ->
                processor.process(
                    new SourceChunk<>(new ChunkRange(0, 0, 1), List.of(1))))
        .isSameAs(expected);
  }
}
