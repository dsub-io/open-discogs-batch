package io.dsub.discogs.batch.job.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.job.progress.SourceChunk;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

class SourceChunkItemStreamReaderUnitTest {

  @Test
  void emitsStableContiguousWindowsAndDelegatesLifecycle() throws Exception {
    @SuppressWarnings("unchecked")
    ItemStreamReader<String> delegate = mock(ItemStreamReader.class);
    when(delegate.read()).thenReturn("a", "b", "c", null);
    SourceChunkItemStreamReader<String> reader =
        new SourceChunkItemStreamReader<>(delegate, 2);
    ExecutionContext executionContext = new ExecutionContext();

    reader.open(executionContext);
    SourceChunk<String> first = reader.read();
    SourceChunk<String> second = reader.read();
    SourceChunk<String> end = reader.read();
    reader.update(executionContext);
    reader.close();

    assertThat(first.range().index()).isZero();
    assertThat(first.range().firstItemIndex()).isZero();
    assertThat(first.values()).containsExactly("a", "b");
    assertThat(second.range().index()).isEqualTo(1);
    assertThat(second.range().firstItemIndex()).isEqualTo(2);
    assertThat(second.values()).containsExactly("c");
    assertThat(end).isNull();
    InOrder lifecycle = inOrder(delegate);
    lifecycle.verify(delegate).open(executionContext);
    lifecycle.verify(delegate).update(executionContext);
    lifecycle.verify(delegate).close();
  }

  @Test
  void rejectsNonPositiveSourceChunkSize() {
    @SuppressWarnings("unchecked")
    ItemStreamReader<String> delegate = mock(ItemStreamReader.class);

    assertThatThrownBy(() -> new SourceChunkItemStreamReader<>(delegate, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be positive");
  }
}
