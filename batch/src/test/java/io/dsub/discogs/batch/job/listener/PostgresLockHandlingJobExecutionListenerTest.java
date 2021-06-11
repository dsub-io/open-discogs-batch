package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.common.entity.base.BaseEntity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reflections.Reflections;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresLockHandlingJobExecutionListenerTest {

  private static final String BASE_PACKAGE = "io.dsub.discogs.common";
  private static final Pattern DROP_PTN =
      Pattern.compile("^ALTER TABLE (\\w*) DROP CONSTRAINT IF EXISTS (\\w*)$");

  private static final Pattern ADD_FK_PTN =
      Pattern.compile(
          "^ALTER TABLE (\\w*) ADD CONSTRAINT (\\w*) FOREIGN KEY \\((\\w*)\\) REFERENCES (\\w*)$");

  private static final Pattern ADD_UQ_PTN =
      Pattern.compile("^ALTER TABLE (\\w*) ADD CONSTRAINT (\\w*) UNIQUE\\(([\\w,]*)\\)$");

  PostgresLockHandlingJobExecutionListener listener;
  JdbcTemplate jdbcTemplate;
  Reflections reflections = new Reflections(BASE_PACKAGE);
  String ddlQuery;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    listener = new PostgresLockHandlingJobExecutionListener(jdbcTemplate);
    listener = Mockito.spy(listener);
    if (ddlQuery == null) {
      ddlQuery = getDDLQuery();
    }
  }

  @Test
  void whenDisableConstraints__ShouldDropAllTargetConstraints() {
    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    // given
    doNothing().when(jdbcTemplate).execute(captor.capture());

    // when
    listener.disableConstraints();

    // then
    List<String> infoLogs = logSpy.getLogsByExactLevelAsString(Level.INFO, true, "io.dsub.discogs");

    assertAll(
        () -> assertThat(String.join("", captor.getAllValues())).contains(getKeywords()),
        () -> assertThat(captor.getAllValues().size()).isEqualTo(getConstraintCount()),
        () -> assertThat(infoLogs.get(infoLogs.size() - 1)).isEqualTo("constraint drop complete"),
        () -> assertThat(infoLogs.get(0)).isEqualTo("begin dropping constraints"),
        () -> assertThat(infoLogs.size() - 2).isEqualTo(captor.getAllValues().size()),
        () ->
            assertAll(
                () -> {
                  for (String query : captor.getAllValues()) {
                    Matcher matcher = DROP_PTN.matcher(query);
                    assertThat(matcher.matches()).isTrue();
                    String tblName = matcher.group(1);
                    String constraint = matcher.group(2);
                    assertThat(ddlQuery).contains(tblName);
                    assertThat(ddlQuery).contains(constraint);
                  }
                }));
  }

  @Test
  void whenEnableConstraints__ShouldDropAllTargetConstraints() {
    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    // given
    doNothing().when(jdbcTemplate).execute(captor.capture());

    // when
    listener.enableConstraints();

    // then
    List<String> infoLogs = logSpy.getLogsByExactLevelAsString(Level.INFO, true, "io.dsub.discogs");
    assertAll(
        () -> assertThat(String.join("", captor.getAllValues())).contains(getKeywords()),
        () -> assertThat(captor.getAllValues().size()).isEqualTo(getConstraintCount() * 2),
        () ->
            assertThat(infoLogs.get(infoLogs.size() - 1)).isEqualTo("constraint recovery complete"),
        () -> assertThat(infoLogs.get(0)).isEqualTo("drop constraints to prevent duplication"),
        () ->
            assertThat(infoLogs.get(getConstraintCount() + 1))
                .isEqualTo("constraint drop complete"),
        () ->
            assertThat(infoLogs.get(getConstraintCount() + 2))
                .isEqualTo("begin recovering constraints"),
        () -> assertThat(infoLogs.size() - 4).isEqualTo(captor.getAllValues().size()),
        () ->
            assertAll(
                () -> {
                  for (String query : captor.getAllValues()) {
                    Matcher m = ADD_FK_PTN.matcher(query);
                    if (m.matches()) {
                      String tblName = m.group(1);
                      String constraintName = m.group(2);
                      String fkColName = m.group(3);
                      String fkTblName = m.group(4);
                      assertThat(ddlQuery).contains(tblName, constraintName, fkColName, fkTblName);
                      continue;
                    }
                    m = ADD_UQ_PTN.matcher(query);
                    if (m.matches()) {
                      String tblName = m.group(1);
                      String constraintName = m.group(2);
                      String[] uqCols = m.group(3).split(",");
                      assertThat(ddlQuery).contains(tblName, constraintName);
                      assertThat(ddlQuery).contains(uqCols);
                      continue;
                    }
                    m = DROP_PTN.matcher(query);
                    if (m.matches()) {
                      assertThat(m.matches()).isTrue();
                      String tblName = m.group(1);
                      String constraint = m.group(2);
                      assertThat(ddlQuery).contains(tblName);
                      assertThat(ddlQuery).contains(constraint);
                      continue;
                    }
                    fail(
                        query
                            + " does not match any of known constraint drop or add query format.");
                  }
                }));
  }

  private String getDDLQuery() {
    ResourceLoader resourceLoader = new FileSystemResourceLoader();
    Resource resource =
        resourceLoader.getResource("src/main/resources/schema/postgresql-schema.sql");
    assertThat(resource).isNotNull();
    String ddlQuery = null;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
      ddlQuery = reader.lines().filter(line -> !line.isBlank()).collect(Collectors.joining("\n"));
    } catch (IOException e) {
      fail(e);
    }
    return ddlQuery;
  }

  private Set<String> getKeywords() {
    return getEntityClasses().stream()
        .map(this::getKeywords)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private int getConstraintCount() {
    return getEntityClasses().stream()
        .mapToInt(entityClass -> getJoinColumnKeywords(entityClass).size())
        .sum();
  }

  private Set<String> getKeywords(Class<?> entityClass) {
    Set<String> keywords = new HashSet<>();
    keywords.add(entityClass.getAnnotation(Table.class).name());
    keywords.addAll(getJoinColumnKeywords(entityClass));
    return keywords;
  }

  private List<String> getJoinColumnKeywords(Class<?> entityClass) {
    return Arrays.stream(entityClass.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(JoinColumn.class))
        .map(field -> field.getAnnotation(JoinColumn.class).name())
        .collect(Collectors.toList());
  }

  private List<Class<?>> getEntityClasses() {
    return reflections.getSubTypesOf(BaseEntity.class).stream()
        .filter(entityClass -> !Modifier.isAbstract(entityClass.getModifiers()))
        .filter(entityClass -> !Modifier.isInterface(entityClass.getModifiers()))
        .collect(Collectors.toList());
  }
}
