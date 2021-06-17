package io.dsub.discogs.batch.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PostgresqlJpaEntityQueryBuilderTest {

  final Pattern selectInsertQueryPattern =
      Pattern.compile(
          "^INSERT INTO (?<targetTable>\\w*)"
              + "\\((?<targetColumns>[\\w,]*)\\) "
              + "SELECT (?<sourceColumns>[\\w,]*) "
              + "FROM (?<sourceTable>\\w*) "
              + "ON CONFLICT \\((?<constraintColumns>[\\w,]*)\\) "
              + "DO UPDATE SET \\((?<updateTargetColumns>[\\w,]*)\\)"
              + "="
              + "\\((?<updateSourceColumns>[\\w.,()]*)\\)$");
  final Pattern selectPattern = Pattern.compile("(SELECT)");
  final Pattern prunePattern =
      Pattern.compile("DELETE FROM \\w+ WHERE NOT EXISTS (\\+? ?\\([\\w =.]*\\) ?) ?(OR NOT EXISTS (\\+? ?\\([\\w =.]*\\) ?))*");
  final Pattern temporaryInsertPattern =
      Pattern.compile(
          "INSERT INTO (?<tableName>\\w+_tmp)\\((?<columns>[\\w,]+)\\) SELECT (?<fields>[\\w:,()]+)");
  PostgresqlJpaEntityQueryBuilder builder = new PostgresqlJpaEntityQueryBuilder();

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenAnEntity__whenGetSelectInsertQuery__ShouldMatchGivenPattern(
      Class<? extends BaseEntity> entityClass) {

    // when
    String query = builder.getSelectInsertQuery(entityClass);
    Matcher matcher = selectInsertQueryPattern.matcher(query);

    // then
    assertAll(
        () -> assertThat(matcher.matches()).isTrue(),
        () -> assertThat(matcher.group(1)).isNotNull().isNotBlank(),
        () -> assertThat(matcher.group(2).split(",")).hasSize(matcher.group(3).split(",").length),
        () -> assertThat(matcher.group(4)).endsWith("_tmp"),
        () -> assertThat(matcher.group(5).split(",")).doesNotContain(matcher.group(6).split(",")));
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityHasLastModifiedField__WhenGetSelectInsertQuery__ShouldMapToNow(
      Class<? extends BaseEntity> entityClass) {

    // given
    Field lastModifiedField = builder.getLastModifiedAtField(entityClass);
    if (lastModifiedField == null) {
      return;
    }

    // when
    String query = builder.getSelectInsertQuery(entityClass);
    Matcher matcher = selectInsertQueryPattern.matcher(query);

    // then
    assertThat(matcher.matches()).isTrue();
    String targetColumns = matcher.group("updateTargetColumns");
    int idx = split(targetColumns).indexOf(builder.getColumnName(lastModifiedField));
    List<String> updateSourceColumns = split(matcher.group("updateSourceColumns"));

    String value = updateSourceColumns.size() == 1 ? "SELECT(NOW())" : "NOW()";
    assertThat(updateSourceColumns.get(idx)).isEqualTo(value);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityHasCreatedAt__WhenGetSelectInsertQuery__ShouldNotUpdate(
      Class<? extends BaseEntity> entityClass) {

    Field createdAtField = builder.getCreatedAtField(entityClass);
    if (createdAtField == null) {
      return;
    }
    String createdAtCol = builder.getColumnName(createdAtField);

    // when
    String query = builder.getSelectInsertQuery(entityClass);
    Matcher matcher = selectInsertQueryPattern.matcher(query);

    // then
    assertThat(matcher.matches()).isTrue();
    List<String> targetColumns = split(matcher.group("updateTargetColumns"));
    assertThat(targetColumns).doesNotContain(createdAtCol);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntity__WhenGetSelectInsertQuery__ShouldNotUpdateId(
      Class<? extends BaseEntity> entityClass) {

    List<String> idColumns = builder.getIdColumns(entityClass);

    // when
    String query = builder.getSelectInsertQuery(entityClass);
    Matcher matcher = selectInsertQueryPattern.matcher(query);

    // then
    assertThat(matcher.matches()).isTrue();
    List<String> targetColumns = split(matcher.group("updateTargetColumns"));
    assertThat(targetColumns).doesNotContain(idColumns.toArray(String[]::new));

    List<String> sourceColumns = split(matcher.group("updateSourceColumns"));
    assertThat(sourceColumns).doesNotContain(idColumns.toArray(String[]::new));
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityHasJoinColumn__WhenGetPruneQuery__ShouldCountAllReferencingColumns(
      Class<? extends BaseEntity> entityClass) {

    // given
    int count = builder.getNotNullableJoinColumnFields(entityClass).size();
    if (count == 0) {
      return;
    }

    // when
    String query = builder.getPruneQuery(entityClass);
    long selectCnt = selectPattern.matcher(query).results().count();
    Matcher m = prunePattern.matcher(query);

    // then
    assertThat(m.matches()).isTrue();
    assertThat(selectCnt).isEqualTo(count);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenAnEntity__whenGetTemporaryInsertQuery__ShouldMatchGivenPattern(
      Class<? extends BaseEntity> entityClass) {

    // when
    String query = builder.getTemporaryInsertQuery(entityClass);
    Matcher m = temporaryInsertPattern.matcher(query);

    // then
    assertThat(m.matches()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenCreatedAtOrLastModifiedAt__whenGetTemporaryInsertQuery__ShouldMapAsNow(
      Class<? extends BaseEntity> entityClass) {
    // given
    Field createdAt = builder.getCreatedAtField(entityClass);
    Field lastModifiedAt = builder.getLastModifiedAtField(entityClass);
    if (createdAt == null || lastModifiedAt == null) {
      return;
    }

    // when
    String query = builder.getTemporaryInsertQuery(entityClass);
    Matcher m = temporaryInsertPattern.matcher(query);

    // then
    assertThat(m.matches()).isTrue();
    assertThat(query)
        .doesNotContain(createdAt.getName(), lastModifiedAt.getName())
        .contains("NOW()");
  }

  private List<String> split(String s) {
    return Arrays.stream(s.split(",")).collect(Collectors.toList());
  }
}
