package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class DefaultJooqMasterMainReleaseItemWriterUnitTest {

  @Test
  void emptyChunkDoesNotAcquireAConnection() {
    DataSource dataSource = mock(DataSource.class);
    DefaultJooqMasterMainReleaseItemWriter writer =
        new DefaultJooqMasterMainReleaseItemWriter(DSL.using(dataSource, SQLDialect.POSTGRES));

    writer.write(new Chunk<>());

    verifyNoInteractions(dataSource);
  }

  @Test
  void rejectsContextsWithoutADataSource() {
    DSLContext context = DSL.using(SQLDialect.POSTGRES);

    DefaultJooqMasterMainReleaseItemWriter writer =
        new DefaultJooqMasterMainReleaseItemWriter(context);

    assertThatThrownBy(
            () ->
                writer.write(
                    new Chunk<>(
                        java.util.List.of(
                            new MasterMainReleaseAssignment(
                                1, 2, LocalDateTime.of(2026, 8, 1, 0, 0))))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a DataSource-backed DSLContext");
  }
}
