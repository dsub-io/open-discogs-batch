package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

class ConvergingRelationItemWriterUnitTest {

  @Test
  void emptySpringChunkDoesNothing() throws Exception {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<UpdatableRecord<?>>> delegate = mock(ItemWriter.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(mock(DataSource.class), delegate);

    writer.write(new Chunk<>());

    verify(delegate, never()).write(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMixedEntityTypesBeforeOpeningADatabaseConnection() {
    @SuppressWarnings("unchecked")
    ItemWriter<Collection<UpdatableRecord<?>>> delegate = mock(ItemWriter.class);
    DataSource dataSource = mock(DataSource.class);
    ConvergingRelationItemWriter writer =
        new ConvergingRelationItemWriter(dataSource, delegate);
    Chunk<RelationSet> mixed =
        new Chunk<>(
            List.of(
                new RelationSet(EntityType.ARTIST, 1, List.of()),
                new RelationSet(EntityType.LABEL, 2, List.of())));

    assertThatThrownBy(() -> writer.write(mixed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("multiple entity types");
  }
}
