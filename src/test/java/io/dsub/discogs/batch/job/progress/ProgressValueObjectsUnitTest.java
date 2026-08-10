package io.dsub.discogs.batch.job.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgressValueObjectsUnitTest {

  @Test
  void chunkRangeRejectsEveryInvalidCoordinate() {
    assertThatThrownBy(() -> new ChunkRange(-1, 0, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ChunkRange(0, -1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ChunkRange(0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sourceChunkRequiresAnExactImmutableSourceRange() {
    ChunkRange range = new ChunkRange(0, 0, 1);
    List<String> values = new ArrayList<>(List.of("value"));
    SourceChunk<String> sourceChunk = new SourceChunk<>(range, values);
    values.clear();

    assertThat(sourceChunk.values()).containsExactly("value");
    assertThatThrownBy(() -> new SourceChunk<String>(null, List.of("value")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("range");
    assertThatThrownBy(() -> new SourceChunk<String>(range, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("values");
    assertThatThrownBy(() -> new SourceChunk<>(range, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("do not match");
  }

  @Test
  void processedChunkCopiesValuesAndRequiresItsRange() {
    ChunkRange range = new ChunkRange(0, 0, 1);
    List<String> values = new ArrayList<>(List.of("value"));
    ProcessedChunk<String> processedChunk = new ProcessedChunk<>(range, values);
    values.clear();

    assertThat(processedChunk.values()).containsExactly("value");
    assertThatThrownBy(() -> new ProcessedChunk<String>(null, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("range");
    assertThatThrownBy(() -> new ProcessedChunk<String>(range, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("values");
  }
}
