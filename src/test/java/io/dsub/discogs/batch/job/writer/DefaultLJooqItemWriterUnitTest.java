package io.dsub.discogs.batch.job.writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class DefaultLJooqItemWriterUnitTest {

  @Test
  void emptyChunkDoesNotCreateDatabaseBatch() {
    DSLContext context = mock(DSLContext.class);
    DefaultLJooqItemWriter<ArtistRecord> writer = new DefaultLJooqItemWriter<>(context);

    writer.write(new Chunk<>());

    verifyNoInteractions(context);
  }
}
