package io.dsub.discogsdata.batch.dto;

import io.dsub.discogsdata.common.exception.UnsupportedOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDtoQueryBuilder implements DtoQueryBuilder {

    private final DtoRegistry dtoRegistry;

    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final String GET_TABLE_NAME = "getTblName";
    private static final String SELECT = "SELECT ";
    private static final String INSERT = "INSERT ";
    private static final String DELETE = "DELETE ";
    private static final String FROM = "FROM ";
    private static final String WHERE = "WHERE ";
    private static final String NOT = "NOT ";
    private static final String EXISTS = "EXISTS ";
    private static final String IF = "IF ";
    private static final String INTO = "INTO ";
    private static final String IGNORE = "IGNORE ";
    private static final String INSERT_INTO = INSERT + INTO;
    private static final String INSERT_IGNORE_INTO = INSERT + IGNORE + INTO;
    private static final String DROP_TBL = "DROP TABLE ";
    private static final String IF_EXISTS = IF + EXISTS;
    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String LIKE = "LIKE ";
    private static final String ON_DUPLICATE_KEY_UPDATE = "ON DUPLICATE KEY UPDATE ";
    private static final String VALUES = "VALUES ";
    private static final String TMP = "_tmp";

    /**
     * builds insert query based on entire fields that class contains.
     *
     * @param clazz target class
     * @return query
     */
    @Override
    public <T extends BaseDTO> String buildInsertQuery(Class<T> clazz, boolean updateIfExists) {

        T item = dtoRegistry.getInstanceOf(clazz);

        List<String> colNames = getColumns(clazz);
        List<String> mappedParams = getMappedParams(clazz);

        if (item.isTimeBaseEntity()) {
            colNames.add("last_modified_at");
            mappedParams.add("NOW()");
        }

        StringBuilder sb = new StringBuilder();
        String tblName = null;

        try {
            Method m = clazz.getMethod(GET_TABLE_NAME);
            if (!m.trySetAccessible()) {
                throw new UnsupportedOperationException("failed to setAccessible to obtain constructor from class " + clazz.getName());
            }
            tblName = (String) m.invoke(item);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        assert (tblName != null);

        if (item.isRelational()) {
            tblName += "_tmp";
        }

        sb.append(INSERT_INTO)
                .append(tblName)
                .append(wrapJoin(colNames))
                .append(VALUES)
                .append(wrapJoin(mappedParams));

        if (updateIfExists) {
            sb.append(ON_DUPLICATE_KEY_UPDATE)
                    .append(getNamedParamSql(clazz));
        }

        return sb.toString();
    }

    @Override
    public <T extends BaseDTO> String buildCreateCloneTableQuery(Class<T> clazz) {
        String tblName = getTableName(clazz);
        return CREATE_TABLE + tblName + TMP + " " + LIKE + tblName;
    }

    @Override
    public <T extends BaseDTO> String buildDropCloneTableQuery(Class<T> clazz) {
        return DROP_TBL + IF_EXISTS + getTableName(clazz) + TMP;
    }

    @Override
    public <T extends BaseDTO> String buildInjectCloneTableQuery(Class<T> clazz) {
        String wholeColumns = String.join(",", getColumns(clazz));
        String tblName = getTableName(clazz);
        return INSERT_IGNORE_INTO +
                tblName +
                wrap(wholeColumns) +
                SELECT +
                wholeColumns +
                " " +
                FROM +
                tblName +
                TMP;
    }

    @Override
    public <T extends BaseDTO> String buildComparePruneCloneTableQuery(Class<T> clazz) {
        String tblName = getTableName(clazz);

        String valuesMappedString = getColumns(clazz).stream()
                .map(column -> tblName + "." + column + "=" + tblName + TMP + "." + column)
                .collect(Collectors.joining(" AND "));

        return DELETE + FROM + tblName + " " + WHERE + NOT + EXISTS +
                wrap(SELECT + 1 + " " + FROM + tblName + TMP + " " + WHERE + valuesMappedString) + ";";
    }

    private String wrapJoin(List<String> strings) {
        return wrap(String.join(",", strings));
    }

    private String wrap(String in) {
        return "(" + in + ") ";
    }

    public List<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    public List<String> getColumns(Class<?> clazz) {
        return getFieldNames(clazz).parallelStream()
                .map(this::toColumnString)
                .collect(Collectors.toList());
    }

    public List<String> getMappedParams(Class<?> clazz) {
        return getFieldNames(clazz).stream()
                .map(field -> ":" + field)
                .collect(Collectors.toList());
    }

    private String getNamedParamSql(Class<?> clazz) {
        return getFieldNames(clazz).parallelStream()
                .map(field -> toColumnString(field) + "=:" + field)
                .collect(Collectors.joining(","));
    }


    // i.e. artistRelease to artist_release
    private String toColumnString(String in) {
        return Arrays.stream(in.split(""))
                .map(s -> {
                    if (UPPER_CASE.matcher(s).matches()) {
                        return "_" + s.toLowerCase(Locale.ROOT);
                    }
                    return s;
                })
                .collect(Collectors.joining(""));
    }

    private String getTableName(Class<? extends BaseDTO> clazz) {
        return dtoRegistry.getInstanceOf(clazz).getTblName();
    }
}
