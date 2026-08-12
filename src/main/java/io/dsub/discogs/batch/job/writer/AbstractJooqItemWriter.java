package io.dsub.discogs.batch.job.writer;

import io.dsub.opendiscogs.jooq.tables.Artist;
import io.dsub.opendiscogs.jooq.tables.Genre;
import io.dsub.opendiscogs.jooq.tables.Label;
import io.dsub.opendiscogs.jooq.tables.Master;
import io.dsub.opendiscogs.jooq.tables.ReleaseItem;
import io.dsub.opendiscogs.jooq.tables.Style;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

public abstract class AbstractJooqItemWriter<T extends UpdatableRecord<?>> implements JooqItemWriter<T> {

  private static final Set<Table<?>> ROOT_TABLES =
      Set.of(Artist.ARTIST, Label.LABEL, Master.MASTER, ReleaseItem.RELEASE_ITEM);
  private static final Set<Table<?>> CANONICAL_KEY_TABLES =
      Set.of(
          Artist.ARTIST,
          Genre.GENRE,
          Label.LABEL,
          Master.MASTER,
          ReleaseItem.RELEASE_ITEM,
          Style.STYLE);

  private final Map<Table<?>, List<Field<?>>> insertFields = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<Field<?>>> constraintFieldsCache = new ConcurrentHashMap<>();
  private final Map<Table<?>, List<Field<?>>> updateFieldsCache = new ConcurrentHashMap<>();

  protected List<Object> getInsertValues(T record) {
    return getInsertFields(record.getTable()).stream()
        .map(field -> field.getValue(record))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  protected List<Field<?>> getInsertFields(Table<?> table) {
    return insertFields.computeIfAbsent(
        table,
        uncachedTable -> {
          List<Field<?>> fields = new ArrayList<>(List.of(uncachedTable.fields()));
          if (!ROOT_TABLES.contains(uncachedTable)) {
            fields.removeIf(field -> field.getName().equals("id"));
          }
          return List.copyOf(fields);
        });
  }

  protected Map<String, Object> getUpdateMap(T record) {
    Map<String, Object> updateValues = new LinkedHashMap<>();
    getUpdateFields(record.getTable())
        .forEach(field -> updateValues.put(field.getName(), field.getValue(record)));
    return updateValues;
  }

  protected List<Field<?>> getConstraintFields(Table<?> table) {
    return constraintFieldsCache.computeIfAbsent(
        table,
        uncachedTable ->
            RelationTableRegistry.find(uncachedTable)
                .map(RelationTableRegistry.RelationTable::conflictFields)
                .orElseGet(() -> generatedConstraintFields(uncachedTable)));
  }

  protected List<Field<?>> getUpdateFields(Table<?> table) {
    return updateFieldsCache.computeIfAbsent(
        table,
        uncachedTable -> {
          List<Field<?>> constraintFields = getConstraintFields(uncachedTable);
          List<Field<?>> mutableFields = RelationTableRegistry.mutableFields(uncachedTable);
          boolean hashIdentity = uncachedTable.field("hash") != null;
          return Arrays.stream(uncachedTable.fields())
              .filter(field -> !constraintFields.contains(field))
              .filter(field -> !field.getName().equals("created_at"))
              .filter(field -> !field.getName().equals("id"))
              .filter(
                  field ->
                      !uncachedTable.equals(Master.MASTER)
                          || !field.getName().equals("main_release_id"))
              .filter(
                  field ->
                      !hashIdentity
                          || field.getName().equals("last_modified_at")
                          || mutableFields.contains(field))
              .toList();
        });
  }

  protected List<Field<?>> getBusinessUpdateFields(Table<?> table) {
    return getUpdateFields(table).stream()
        .filter(field -> !field.getName().equals("last_modified_at"))
        .collect(Collectors.toList());
  }

  private List<Field<?>> generatedConstraintFields(Table<?> table) {
    if (!CANONICAL_KEY_TABLES.contains(table)) {
      throw new IllegalArgumentException(
          "table has no registered canonical conflict key: " + table.getName());
    }
    List<Field<?>> fields =
        new ArrayList<>(table.getPrimaryKey().getFields());
    if (!ROOT_TABLES.contains(table)) {
      fields.removeIf(field -> field.getName().equals("id"));
    }
    return List.copyOf(fields);
  }
}
