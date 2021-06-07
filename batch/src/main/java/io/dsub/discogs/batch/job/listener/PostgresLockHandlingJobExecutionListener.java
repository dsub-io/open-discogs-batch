package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

@Slf4j
@RequiredArgsConstructor
public class PostgresLockHandlingJobExecutionListener extends LockHandlingJobExecutionListener {

  protected static final String BASE_PACKAGE = "io.dsub.discogs.common.entity";
  private static final String ADD_CONSTRAINT_SQL = "ALTER TABLE %s ADD CONSTRAINT %s";
  private static final String DROP_CONSTRAINT_SQL = "ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s";
  private static final String ADD_FK_FORMAT = "%s FOREIGN KEY (%s) REFERENCES %s";
  private static final String ADD_UNIQUE = "%s UNIQUE(%s)";
  private static final String FK_FORMAT = "fk_%s_%s_%s";
  private static final String ASSERTION_ERR_MSG_FMT = "query <{}> does not match to known formats.";
  private static final String DROP_CONSTRAINT_LOG_FMT = "dropping constraint <{}> from table <{}>.";
  private static final String ADD_FK_LOG_FMT = "adding foreign key constraint <{}> to table <{}> referencing table <{}>.";
  private static final String ADD_UQ_LOG_FMT = "adding unique constraint <{}> to table <{}> containing columns: <{}>.";

  private static final Pattern DROP_PTN = Pattern.compile(
      "^ALTER TABLE (\\w*) DROP CONSTRAINT IF EXISTS (\\w*)$"
  );

  private static final Pattern ADD_FK_PTN = Pattern.compile(
      "^ALTER TABLE (\\w*) ADD CONSTRAINT (\\w*) FOREIGN KEY \\((\\w*)\\) REFERENCES (\\w*)$"
  );

  private static final Pattern ADD_UQ_PTN = Pattern.compile(
      "^ALTER TABLE (\\w*) ADD CONSTRAINT (\\w*) UNIQUE\\(([\\w,]*)\\)$"
  );

  private final JdbcTemplate jdbcTemplate;

  private final Reflections reflections = new Reflections(BASE_PACKAGE);
  private final List<Class<?>> classes = getJpaClasses();

  @Override
  protected void disableConstraints() {
    log.info("begin dropping constraints");
    classes.stream()
        .map(this::getDropConstraintQueries)
        .forEach(queries -> queries.forEach(query -> {
          logDropConstraint(query);
          jdbcTemplate.execute(query);
        }));
    log.info("constraint drop complete");
  }

  @Override
  protected void enableConstraints() {
    log.info("drop constraints to prevent duplication");
    classes.stream()
        .map(this::getDropConstraintQueries)
        .forEach(queries -> queries.forEach(query -> {
          logDropConstraint(query);
          jdbcTemplate.execute(query);
        }));
    log.info("constraint drop complete");
    log.info("begin recovering constraints");
    classes.stream()
        .map(this::getAddConstraintQueries)
        .forEach(queries -> queries.forEach(query -> {
          logAddConstraintSql(query);
          jdbcTemplate.execute(query);
        }));
    log.info("constraint recovery complete");
  }

  private void logAddConstraintSql(String query) {
    if (logIfQueryAddsFK(query)) {
      return;
    }
    if (logIfQueryAddsUQ(query)) {
      return;
    }
    log.error(ASSERTION_ERR_MSG_FMT, query);
  }

  private boolean logIfQueryAddsFK(String query) {
    Matcher m = ADD_FK_PTN.matcher(query);
    if (m.matches()) {
      String tblName = m.group(1);
      String constraint = m.group(2);
      String fkTblName = m.group(4);
      log.info(ADD_FK_LOG_FMT, tblName, constraint, fkTblName);
      return true;
    }
    return false;
  }

  private boolean logIfQueryAddsUQ(String query) {
    Matcher m;
    m = ADD_UQ_PTN.matcher(query);
    if (m.matches()) {
      String tblName = m.group(1);
      String constraint = m.group(2);
      String uqCols = m.group(3);
      log.info(ADD_UQ_LOG_FMT, tblName, constraint, uqCols);
      return true;
    }
    return false;
  }

  private void logDropConstraint(String query) {
    Matcher m = DROP_PTN.matcher(query);
    Assert.isTrue(m.matches(), String.format(ASSERTION_ERR_MSG_FMT, query));
    String tableName = m.group(1);
    String constraintName = m.group(2);
    log.info(DROP_CONSTRAINT_LOG_FMT, constraintName, tableName);
  }

  private List<Class<?>> getJpaClasses() {
    return reflections.getSubTypesOf(BaseEntity.class)
        .stream()
        .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) && !Modifier
            .isInterface(clazz.getModifiers()))
        .collect(Collectors.toList());
  }

  private List<String> getAddConstraintQueries(Class<?> entityClass) {
    List<Field> joinColumnFields = getJoinColumnFields(entityClass);
    String tblName = entityClass.getAnnotation(Table.class).name();

    List<String> queries = joinColumnFields.stream()
        .map(field -> getAddForeignKeyFormat(field, tblName))
        .map(addFk -> String.format(ADD_CONSTRAINT_SQL, tblName, addFk))
        .collect(Collectors.toList());

//    queries.addAll(getAddUniqueQueries(entityClass));
    return queries;
  }

  private List<String> getDropConstraintQueries(Class<?> entityClass) {
    String tblName = entityClass.getAnnotation(Table.class).name();

    List<String> constraintNames = getJoinColumnFields(entityClass).stream()
        .map(field -> getForeignKeyConstraintName(field, tblName))
        .collect(Collectors.toList());

//    constraintNames.addAll(getUniqueConstraintNames(entityClass));

    return constraintNames.stream()
        .map(name -> String.format(DROP_CONSTRAINT_SQL, tblName, name))
        .collect(Collectors.toList());
  }

  private List<String> getUniqueConstraintNames(Class<?> entityClass) {
    return Arrays.stream(entityClass.getAnnotation(Table.class).uniqueConstraints())
        .map(UniqueConstraint::name)
        .collect(Collectors.toList());
  }

  private String getForeignKeyConstraintName(Field field, String tblName) {
    String columnName = field.getAnnotation(JoinColumn.class).name();
    String fkTblName = getForeignKeyEntityTableName(field);
    return String.format(FK_FORMAT, tblName, columnName, fkTblName);
  }

  private String getAddForeignKeyFormat(Field field, String tblName) {
    String columnName = field.getAnnotation(JoinColumn.class).name();
    String fkTblName = getForeignKeyEntityTableName(field);
    String fkName = String.format(FK_FORMAT, tblName, columnName, fkTblName);
    return String.format(ADD_FK_FORMAT, fkName, columnName, fkTblName);
  }

  private List<String> getAddUniqueQueries(Class<?> entityClass) {
    Table table = entityClass.getAnnotation(Table.class);
    return Arrays.stream(table.uniqueConstraints())
        .map(unique -> String
            .format(ADD_UNIQUE, unique.name(), String.join(",", unique.columnNames())))
        .map(q -> String.format(ADD_CONSTRAINT_SQL, table.name(), q))
        .collect(Collectors.toList());
  }

  private String getForeignKeyEntityTableName(Field joinColumnField) {
    return joinColumnField.getType().getAnnotation(Table.class).name();
  }

  private List<Field> getJoinColumnFields(Class<?> entityClass) {
    return Arrays.stream(entityClass.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(JoinColumn.class))
        .collect(Collectors.toList());
  }
}
