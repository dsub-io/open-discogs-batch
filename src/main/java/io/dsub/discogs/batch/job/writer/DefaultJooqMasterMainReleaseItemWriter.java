package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.opendiscogs.jooq.tables.Master;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.ConnectionProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DefaultJooqMasterMainReleaseItemWriter
    implements ItemWriter<MasterMainReleaseAssignment> {

  private static final int MAX_QUERIES_PER_ASSIGNMENT = 2;
  private static final String INVALID_CONNECTION_PROVIDER_MESSAGE =
      "master main release writer requires a DataSource-backed DSLContext";

  private final DSLContext context;

  public DefaultJooqMasterMainReleaseItemWriter(DSLContext context) {
    this.context = context;
  }

  @Override
  public void write(Chunk<? extends MasterMainReleaseAssignment> items) {
    if (items.isEmpty()) {
      return;
    }

    DataSource dataSource = dataSource(context.configuration().connectionProvider());
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      DSLContext transactionContext = DSL.using(context.configuration().derive(connection));
      List<Query> updates =
          new ArrayList<>(items.size() * MAX_QUERIES_PER_ASSIGNMENT);
      for (MasterMainReleaseAssignment assignment : items) {
        updates.add(clearStaleQuery(transactionContext, assignment));
        if (assignment.targetMasterId() != null) {
          updates.add(setCurrentQuery(transactionContext, assignment));
        }
      }
      transactionContext.batch(updates).execute();
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private Query clearStaleQuery(
      DSLContext transactionContext, MasterMainReleaseAssignment assignment) {
    Condition staleMapping = Master.MASTER.MAIN_RELEASE_ID.eq(assignment.releaseId());
    if (assignment.targetMasterId() != null) {
      staleMapping = staleMapping.and(Master.MASTER.ID.ne(assignment.targetMasterId()));
    }
    return transactionContext
        .update(Master.MASTER)
        .set(Master.MASTER.LAST_MODIFIED_AT, assignment.observedAt())
        .setNull(Master.MASTER.MAIN_RELEASE_ID)
        .where(staleMapping);
  }

  private Query setCurrentQuery(
      DSLContext transactionContext, MasterMainReleaseAssignment assignment) {
    return transactionContext
        .update(Master.MASTER)
        .set(Master.MASTER.LAST_MODIFIED_AT, assignment.observedAt())
        .set(Master.MASTER.MAIN_RELEASE_ID, assignment.releaseId())
        .where(Master.MASTER.ID.eq(assignment.targetMasterId()))
        .and(Master.MASTER.MAIN_RELEASE_ID.isDistinctFrom(assignment.releaseId()));
  }

  private static DataSource dataSource(ConnectionProvider connectionProvider) {
    if (connectionProvider instanceof DataSourceConnectionProvider provider) {
      return provider.dataSource();
    }
    throw new IllegalArgumentException(INVALID_CONNECTION_PROVIDER_MESSAGE);
  }
}
