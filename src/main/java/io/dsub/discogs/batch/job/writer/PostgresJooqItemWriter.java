package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.common.jooq.postgres.tables.Artist;
import io.dsub.discogs.common.jooq.postgres.tables.Label;
import io.dsub.discogs.common.jooq.postgres.tables.Master;
import io.dsub.discogs.common.jooq.postgres.tables.ReleaseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PostgresJooqItemWriter<T extends UpdatableRecord<?>> implements JooqItemWriter<T> {

    private final DSLContext context;

    private static final Map<Table<?>, List<Field<?>>> INSERT_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Table<?>, List<String>> INSERT_FIELD_NAMES= new ConcurrentHashMap<>();
    private static final Map<Table<?>, List<String>> KEYS_TO_DELETE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Table<?>, List<Field<?>>> CONSTRAINT_FIELDS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Table<?>, List<Field<?>>> UPDATE_FIELDS_CACHE = new ConcurrentHashMap<>();

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
     * map values from record into a full array with null as null value.
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
            return context.insertInto(record.getTable(), getInsertFields(record.getTable()))
                    .values(getInsertValues(record))
                    .onConflict(constraintFields)
                    .doNothing();
        }

        return context.insertInto(record.getTable(), getInsertFields(record.getTable()))
                .values(getInsertValues(record))
                .onConflict(constraintFields)
                .doUpdate()
                .set(updateMap);
    }

    private List<Object> getInsertValues(T record) {

        List<String> fieldNames;

        if (INSERT_FIELD_NAMES.containsKey(record.getTable())) {
            fieldNames = INSERT_FIELD_NAMES.get(record.getTable());
        } else {
            fieldNames = getInsertFields(record.getTable()).stream()
                    .map(Field::getName)
                    .collect(Collectors.toList());
            INSERT_FIELD_NAMES.put(record.getTable(), fieldNames);
        }

        return record.intoMap().entrySet().stream()
                .filter(entry -> fieldNames.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

    }

    private List<Field<?>> getInsertFields(Table<?> table) {

        if (INSERT_FIELDS.containsKey(table)) {
            return INSERT_FIELDS.get(table);
        }

        List<Field<?>> fields = new ArrayList<>(List.of(table.fields()));

        if (!table.equals(Artist.ARTIST) && !table.equals(Label.LABEL) && !table.equals(Master.MASTER) && !table.equals(ReleaseItem.RELEASE_ITEM)) {
            fields = fields.stream().filter(field -> !field.getName().equals("id")).collect(Collectors.toList());
        }

        INSERT_FIELDS.put(table, fields);

        return fields;
    }

    private Map<?, ?> getUpdateMap(T record) {
        Map<?, ?> map = record.intoMap();
        List<Field<?>> fieldsToUpdate = getUpdateFields(record.getTable());
        if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
            return map;
        }

        List<String> keysToDelete;

        if (KEYS_TO_DELETE_CACHE.containsKey(record.getTable())) {
            keysToDelete = KEYS_TO_DELETE_CACHE.get(record.getTable());
        } else {
            List<String> updateFieldNames = fieldsToUpdate.stream().map(Field::getName).collect(Collectors.toList());
            keysToDelete = map.keySet().stream().map(Object::toString).filter(key -> !updateFieldNames.contains(key)).collect(Collectors.toList());
            KEYS_TO_DELETE_CACHE.put(record.getTable(), keysToDelete);
        }

        for (String key : keysToDelete) {
            map.remove(key);
        }
        return map;
    }

    private List<Field<?>> getConstraintFields(Table<?> table) {
        if (CONSTRAINT_FIELDS_CACHE.containsKey(table)) {
            return CONSTRAINT_FIELDS_CACHE.get(table);
        }
        List<Field<?>> fields = table.getKeys().stream()
                .map(Key::getFields)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (!table.equals(Artist.ARTIST) && !table.equals(Label.LABEL) && !table.equals(Master.MASTER) && !table.equals(ReleaseItem.RELEASE_ITEM)) {
            fields = fields.stream()
                    .filter(field -> !field.getName().equals("id"))
                    .collect(Collectors.toList());
        }
        CONSTRAINT_FIELDS_CACHE.put(table, fields);
        return fields;
    }

    private List<Field<?>> getUpdateFields(Table<?> table) {
        if (UPDATE_FIELDS_CACHE.containsKey(table)) {
            return UPDATE_FIELDS_CACHE.get(table);
        }
        List<Field<?>> constraintFields = getConstraintFields(table);
        List<Field<?>> updateFields = Arrays.stream(table.fields())
                .filter(field -> !constraintFields.contains(field))
                .filter(field -> !field.getName().equals("created_at") && !field.getName().equals("id")) // Predicate<Field<?>> as filter.
                .filter(field -> !table.equals(Master.MASTER) || !field.getName().equals("main_release_id"))
                .filter(field -> {
                    if (table.field("hash") == null) {
                        return true;
                    }
                    return field.getName().equals("last_modified_at");
                })
                .collect(Collectors.toList());

        UPDATE_FIELDS_CACHE.put(table, updateFields);
        return updateFields;
    }
}
