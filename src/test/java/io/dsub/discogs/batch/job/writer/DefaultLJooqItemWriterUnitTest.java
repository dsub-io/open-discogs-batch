package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
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

  @Test
  void exposesThePostgresUpsertQueryForARecord() {
    DSLContext context = DSL.using(SQLDialect.POSTGRES);
    DefaultLJooqItemWriter<ArtistRecord> writer = new DefaultLJooqItemWriter<>(context);
    ArtistRecord record = new ArtistRecord();
    record.setId(7);
    record.setName("Artist");

    Query query = writer.getQuery(record);

    assertThat(query.getSQL()).contains("insert into", "on conflict");
  }

  @Test
  void rejectsAContextWithoutADataSourceBeforeWriting() {
    DefaultLJooqItemWriter<ArtistRecord> writer =
        new DefaultLJooqItemWriter<>(DSL.using(SQLDialect.POSTGRES));

    assertThatThrownBy(() -> writer.write(new Chunk<>(java.util.List.of(new ArtistRecord()))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("jOOQ item writer requires a DataSource-backed DSLContext");
  }
}
