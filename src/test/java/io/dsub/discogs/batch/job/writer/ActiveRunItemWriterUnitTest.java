package io.dsub.discogs.batch.job.writer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class ActiveRunItemWriterUnitTest {

  @Test
  void fencesAfterDelegateWrite() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<String> delegate = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    ActiveRunItemWriter<String> writer = new ActiveRunItemWriter<>(delegate, progressStore, 3L);
    Chunk<String> items = new Chunk<>(List.of("value"));

    writer.write(items);

    verify(delegate).write(items);
    verify(progressStore).fenceActiveRun(3L);
  }

  @Test
  void delegateFailureDoesNotAttemptTheFence() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<String> delegate = mock(ItemWriter.class);
    ImportProgressStore progressStore = mock(ImportProgressStore.class);
    ActiveRunItemWriter<String> writer = new ActiveRunItemWriter<>(delegate, progressStore, 3L);
    Chunk<String> items = new Chunk<>(List.of("value"));
    doThrow(new IOException("write failed")).when(delegate).write(items);

    try {
      writer.write(items);
    } catch (IOException ignored) {
      // Expected delegate failure.
    }

    verify(progressStore, never()).fenceActiveRun(3L);
  }
}
