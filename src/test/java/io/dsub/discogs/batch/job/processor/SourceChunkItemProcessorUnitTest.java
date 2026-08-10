package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.util.List;
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
}
