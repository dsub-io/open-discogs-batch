package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class ProcessedChunkItemWriterUnitTest {

  @Test
  void preservesEachSourceChunkAsOneDelegateBatch() throws Exception {
    RecordingWriter delegate = new RecordingWriter();
    ProcessedChunkItemWriter<Integer> writer = new ProcessedChunkItemWriter<>(delegate);

    writer.write(
        new Chunk<>(
            List.of(
                new ProcessedChunk<>(new ChunkRange(0, 0, 2), List.of(1, 2)),
                new ProcessedChunk<>(new ChunkRange(1, 2, 1), List.of(3)))));

    assertThat(delegate.batches).containsExactly(List.of(1, 2), List.of(3));
  }

  private static final class RecordingWriter implements ItemWriter<Integer> {
    private final List<List<Integer>> batches = new ArrayList<>();

    @Override
    public void write(Chunk<? extends Integer> items) {
      batches.add(List.copyOf(items.getItems()));
    }
  }
}
