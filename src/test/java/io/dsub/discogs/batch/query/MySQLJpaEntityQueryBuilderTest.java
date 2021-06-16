package io.dsub.discogs.batch.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MySQLJpaEntityQueryBuilderTest {

  final Pattern selectInsertQueryPattern =
      Pattern.compile(
          "INSERT INTO (?<tableName>\\w+)\\((?<columns>[\\w,]+)\\) SELECT (\\2) FROM (\\1_tmp) "
              + "ON DUPLICATE KEY UPDATE (?<updateColumns>,?\\w+=[\\w().]+)*");
  final Pattern selectInsertIgnoreQueryPattern =
      Pattern.compile(
          "INSERT IGNORE INTO (?<tableName>\\w+)\\((?<columns>[\\w,]+)\\) SELECT \\2 FROM \\1_tmp");
  final Pattern selectPattern = Pattern.compile("(SELECT)");
  final Pattern prunePattern =
      Pattern.compile("DELETE FROM \\w+ WHERE (\\+? ?\\([\\w =.]*\\) ?)* < (\\d+)");
  final Pattern temporaryInsertPattern =
      Pattern.compile(
          "INSERT INTO (?<tableName>\\w+_tmp)\\((?<columns>[\\w,]+)\\) SELECT (?<fields>[\\w:,()]+)");
  MySQLJpaEntityQueryBuilder builder = new MySQLJpaEntityQueryBuilder();

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
  void givenCreatedAtOrLastModifiedAt__whenGetTempInsertQuery__ShouldMapAsNow(
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
    assertThat(query).doesNotContain(createdAt.getName(), lastModifiedAt.getName());
    assertThat(query).contains("NOW()");
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntity__WhenGetSelectInsertQuery__ShouldNotUpdateColumnsWithAnyConstraints(
      Class<? extends BaseEntity> entityClass) {
    // given
    List<String> constraintColumns = builder.getConstraintColumns(entityClass);
    constraintColumns.addAll(builder.getIdColumns(entityClass));
    Pattern p = getMatchingSelectInsertPattern(entityClass);

    // when
    String query = builder.getSelectInsertQuery(entityClass);
    Matcher matcher = p.matcher(query);

    // then
    assertThat(matcher.matches()).isTrue();
    if (p == selectInsertIgnoreQueryPattern) {
      return;
    }
    String updateColumns = matcher.group("updateColumns");
    assertThat(updateColumns).doesNotContain(constraintColumns);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityHasJoinColumn__WhenGetPruneQuery__ShouldCountAllReferencingColumns(
      Class<? extends BaseEntity> entityClass) {

    // given
    int count = builder.getJoinColumns(entityClass).size();
    if (count == 0) {
      return;
    }

    // when
    String query = builder.getPruneQuery(entityClass);
    long selectCnt = selectPattern.matcher(query).results().count();
    Matcher m = prunePattern.matcher(query);

    // then
    assertThat(m.matches()).isTrue();
    long expectedCnt = Long.parseLong(m.group(2));
    assertThat(selectCnt).isEqualTo(expectedCnt);
  }

  private Pattern getMatchingSelectInsertPattern(Class<? extends BaseEntity> entityClass) {
    boolean isUpdateRequired = builder.getUpdateColumns(entityClass).size() > 0;
    if (isUpdateRequired) {
      return selectInsertQueryPattern;
    }
    return selectInsertIgnoreQueryPattern;
  }
}
