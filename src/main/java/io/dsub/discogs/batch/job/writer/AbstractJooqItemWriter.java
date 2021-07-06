package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.jooq.tables.Artist;
import io.dsub.discogs.jooq.tables.Label;
import io.dsub.discogs.jooq.tables.Master;
import io.dsub.discogs.jooq.tables.ReleaseItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.Key;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

public abstract class AbstractJooqItemWriter<T extends UpdatableRecord<?>> implements JooqItemWriter<T> {

  private final Map<Table<?>, List<Field<?>>> insertFields = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<String>> insertFieldNames = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<String>> keysToDeleteCache = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<Field<?>>> constraintFieldsCache = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<Field<?>>> updateFieldsCache = new ConcurrentHashMap<>();

  protected List<Object> getInsertValues(T record) {

    List<String> fieldNames;

    if (insertFieldNames.containsKey(record.getTable())) {
      fieldNames = insertFieldNames.get(record.getTable());
    } else {
      fieldNames =
          getInsertFields(record.getTable()).stream()
              .map(Field::getName)
              .collect(Collectors.toList());
      insertFieldNames.put(record.getTable(), fieldNames);
    }

    return record.intoMap().entrySet().stream()
        .filter(entry -> fieldNames.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  protected List<Field<?>> getInsertFields(Table<?> table) {
    if (insertFields.containsKey(table)) {
      return insertFields.get(table);
    }

    List<Field<?>> fields = new ArrayList<>(List.of(table.fields()));

    if (!table.equals(Artist.ARTIST)
        && !table.equals(Label.LABEL)
        && !table.equals(Master.MASTER)
        && !table.equals(ReleaseItem.RELEASE_ITEM)) {
      fields = fields.stream()
          .filter(field -> !field.getName().equals("id"))
          .collect(Collectors.toList());
    }

    insertFields.put(table, fields);

    return fields;
  }

  protected Map<?, ?> getUpdateMap(T record) {
    Map<?, ?> map = record.intoMap();
    List<Field<?>> fieldsToUpdate = getUpdateFields(record.getTable());
    if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
      return map;
    }

    List<String> keysToDelete;

    if (keysToDeleteCache.containsKey(record.getTable())) {
      keysToDelete = keysToDeleteCache.get(record.getTable());
    } else {
      List<String> updateFieldNames =
          fieldsToUpdate.stream().map(Field::getName).collect(Collectors.toList());
      keysToDelete =
          map.keySet().stream()
              .map(Object::toString)
              .filter(key -> !updateFieldNames.contains(key))
              .collect(Collectors.toList());
      keysToDeleteCache.put(record.getTable(), keysToDelete);
    }

    for (String key : keysToDelete) {
      map.remove(key);
    }
    return map;
  }

  protected List<Field<?>> getConstraintFields(Table<?> table) {
    if (constraintFieldsCache.containsKey(table)) {
      return constraintFieldsCache.get(table);
    }
    List<Field<?>> fields =
        table.getKeys().stream()
            .map(Key::getFields)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    if (!table.equals(Artist.ARTIST)
        && !table.equals(Label.LABEL)
        && !table.equals(Master.MASTER)
        && !table.equals(ReleaseItem.RELEASE_ITEM)) {
      fields =
          fields.stream()
              .filter(field -> !field.getName().equals("id"))
              .collect(Collectors.toList());
    }
    constraintFieldsCache.put(table, fields);
    return fields;
  }

  protected List<Field<?>> getUpdateFields(Table<?> table) {
    if (updateFieldsCache.containsKey(table)) {
      return updateFieldsCache.get(table);
    }
    List<Field<?>> constraintFields = getConstraintFields(table);
    List<Field<?>> updateFields =
        Arrays.stream(table.fields())
            .filter(field -> !constraintFields.contains(field))
            .filter(
                field ->
                    !field.getName().equals("created_at")
                        && !field.getName().equals("id")) // Predicate<Field<?>> as filter.
            .filter(
                field -> !table.equals(Master.MASTER) || !field.getName().equals("main_release_id"))
            .filter(
                field -> {
                  if (table.field("hash") == null) {
                    return true;
                  }
                  return field.getName().equals("last_modified_at");
                })
            .collect(Collectors.toList());

    updateFieldsCache.put(table, updateFields);
    return updateFields;
  }
}
