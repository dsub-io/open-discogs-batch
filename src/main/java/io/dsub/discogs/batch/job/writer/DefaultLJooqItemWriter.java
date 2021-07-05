package io.dsub.discogs.batch.job.writer;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.UpdatableRecord;

@Slf4j
@RequiredArgsConstructor
public class DefaultLJooqItemWriter<T extends UpdatableRecord<?>> extends AbstractJooqItemWriter<T> {

  private final DSLContext context;

  @Override
  public void write(List<? extends T> items) {
    if (items.isEmpty()) {
      return;
    }
    Query q = this.getQuery(items.get(0));
    BatchBindStep batch = context.batch(q);

    items.forEach(record -> batch.bind(mapValues(record)));
    batch.execute();
  }

  /**
   * map values from record into a full array
   *
   * @param record to be parsed into array
   * @return values
   */
  private Object[] mapValues(T record) {
    List<Object> values = getInsertValues(record);
    getUpdateFields(record.getTable()).forEach(field -> values.add(field.getValue(record)));
    return values.toArray();
  }

  @Override
  public Query getQuery(T record) {
    List<Field<?>> constraintFields = getConstraintFields(record.getTable());
    List<Field<?>> fieldsToUpdate = getUpdateFields(record.getTable());
    Map<?, ?> updateMap = getUpdateMap(record);

    if (fieldsToUpdate.isEmpty()) {
      return context
          .insertInto(record.getTable(), getInsertFields(record.getTable()))
          .values(getInsertValues(record))
          .onConflict(constraintFields)
          .doNothing();
    }

    return context
        .insertInto(record.getTable(), getInsertFields(record.getTable()))
        .values(getInsertValues(record))
        .onConflict(constraintFields)
        .doUpdate()
        .set(updateMap);
  }
}
