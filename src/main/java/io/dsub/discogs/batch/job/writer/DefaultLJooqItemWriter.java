package io.dsub.discogs.batch.job.writer;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.TableRecord;
import org.jooq.ConnectionProvider;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class DefaultLJooqItemWriter<T extends TableRecord<?>> extends AbstractJooqItemWriter<T> {

  private static final String INVALID_CONNECTION_PROVIDER_MESSAGE =
      "jOOQ item writer requires a DataSource-backed DSLContext";

  private final DSLContext context;

  public DefaultLJooqItemWriter(DSLContext context) {
    this.context = context;
  }

  @Override
  public void write(Chunk<? extends T> items) {
    if (items.isEmpty()) {
      return;
    }
    DataSource dataSource = dataSource(context.configuration().connectionProvider());
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      DSLContext transactionContext = DSL.using(context.configuration().derive(connection));
      Query query = getQuery(items.getItems().getFirst(), transactionContext);
      BatchBindStep batch = transactionContext.batch(query);
      items.forEach(record -> batch.bind(getBindValues(record)));
      batch.execute();
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Query getQuery(T record) {
    return getQuery(record, context);
  }

  private Query getQuery(T record, DSLContext executionContext) {
    List<Field<?>> constraintFields = getConstraintFields(record.getTable());
    List<Field<?>> fieldsToUpdate = getUpdateFields(record.getTable());
    List<Field<?>> businessFieldsToUpdate = getBusinessUpdateFields(record.getTable());
    Map<String, Object> updateMap = getUpdateMap(record);

    if (businessFieldsToUpdate.isEmpty()) {
      return executionContext
          .insertInto(record.getTable(), getInsertFields(record.getTable()))
          .values(getInsertValues(record))
          .onConflict(constraintFields)
          .doNothing();
    }

    Condition changed = DSL.falseCondition();
    for (Field<?> field : businessFieldsToUpdate) {
      changed = changed.or(isDistinctFromExcluded(field));
    }

    return executionContext
        .insertInto(record.getTable(), getInsertFields(record.getTable()))
        .values(getInsertValues(record))
        .onConflict(constraintFields)
        .doUpdate()
        .set(updateMap)
        .where(changed);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Condition isDistinctFromExcluded(Field<?> field) {
    Field untyped = field;
    return untyped.isDistinctFrom(DSL.excluded(untyped));
  }

  private static DataSource dataSource(ConnectionProvider connectionProvider) {
    if (connectionProvider instanceof DataSourceConnectionProvider provider) {
      return provider.dataSource();
    }
    throw new IllegalArgumentException(INVALID_CONNECTION_PROVIDER_MESSAGE);
  }
}
