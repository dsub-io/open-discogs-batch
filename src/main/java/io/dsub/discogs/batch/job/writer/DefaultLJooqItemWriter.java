package io.dsub.discogs.batch.job.writer;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.springframework.batch.infrastructure.item.Chunk;

@Slf4j
@RequiredArgsConstructor
public class DefaultLJooqItemWriter<T extends UpdatableRecord<?>> extends AbstractJooqItemWriter<T> {

  private final DSLContext context;

  @Override
  public void write(Chunk<? extends T> items) {
    if (items.isEmpty()) {
      return;
    }
    Query q = this.getQuery(items.getItems().getFirst());
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
    if (!getBusinessUpdateFields(record.getTable()).isEmpty()) {
      getUpdateFields(record.getTable()).forEach(field -> values.add(field.getValue(record)));
    }
    return values.toArray();
  }

  @Override
  public Query getQuery(T record) {
    List<Field<?>> constraintFields = getConstraintFields(record.getTable());
    List<Field<?>> fieldsToUpdate = getUpdateFields(record.getTable());
    List<Field<?>> businessFieldsToUpdate = getBusinessUpdateFields(record.getTable());
    Map<?, ?> updateMap = getUpdateMap(record);

    if (fieldsToUpdate.isEmpty() || businessFieldsToUpdate.isEmpty()) {
      return context
          .insertInto(record.getTable(), getInsertFields(record.getTable()))
          .values(getInsertValues(record))
          .onConflict(constraintFields)
          .doNothing();
    }

    Condition changed = DSL.falseCondition();
    for (Field<?> field : businessFieldsToUpdate) {
      changed = changed.or(isDistinctFromExcluded(field));
    }

    return context
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
}
