package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class CollectionItemWriterUnitTest {

  @Test
  void flushesExpandedItemsAtTheConfiguredBatchBoundary() throws Exception {
    RecordingWriter delegate = new RecordingWriter();
    CollectionItemWriter<Integer> writer = new CollectionItemWriter<>(delegate, 2);

    writer.write(new Chunk<>(List.of(List.of(1, 2, 3), List.of(4, 5))));

    assertThat(delegate.batches).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
  }

  private static final class RecordingWriter implements ItemWriter<Integer> {
    private final List<List<Integer>> batches = new ArrayList<>();

    @Override
    public void write(Chunk<? extends Integer> items) {
      batches.add(List.copyOf(items.getItems()));
    }
  }
}
