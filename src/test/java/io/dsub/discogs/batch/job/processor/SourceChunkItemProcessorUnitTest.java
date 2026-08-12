package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.CompletedChunkInventory;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SourceChunkItemProcessorUnitTest {

  @Test
  void preservesRangeWhileFilteringNullDelegateResults() throws Exception {
    SourceChunkItemProcessor<Integer, String> processor =
        new SourceChunkItemProcessor<>(value -> value == 2 ? null : "value-" + value);
    ChunkRange range = new ChunkRange(0, 0, 3);

    ProcessedChunk<String> processed =
        processor.process(new SourceChunk<>(range, List.of(1, 2, 3)));

    assertThat(processed.range()).isEqualTo(range);
    assertThat(processed.values()).containsExactly("value-1", "value-3");
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
}
