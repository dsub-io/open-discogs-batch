package io.dsub.discogsdata.batch.query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.dsub.discogsdata.batch.query.JpaEntityExtractorTest.TestEntity;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

class PostgresqlJpaEntityQueryBuilderTest {

  PostgresqlJpaEntityQueryBuilder builder =
      new PostgresqlJpaEntityQueryBuilder();

  @Test
  void ifUniqueConstraintExistsTwice__AndAutoId__ShouldContainNonDuplicatedColumnsOnWhereClause() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityTwo.class);

    int startLen = "ON CONFLICT (".length();
    int startIdx = query.indexOf("ON CONFLICT (");
    int lastIdx = query.indexOf(") DO UPDATE");
    String[] parts = query.substring(startIdx + startLen, lastIdx).split(",");

    String[] whereParts =
        Arrays.stream(
            query.substring(
                query.indexOf("WHERE ") + 6).split(" AND "))
            .map(s -> s.split("=")[0])
            .toArray(String[]::new);

    // then
    for (int i = 0; i < whereParts.length; i++) {
      assertThat(whereParts[i]).contains(parts[i]);
    }
    assertThat(whereParts.length).isEqualTo(parts.length);
  }

  @Test
  void ifUniqueConstraintExists__AndAutoId__ShouldContainConstraintsOnWhereClause() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityOne.class);
    String target = query.substring(query.indexOf("WHERE"));

    // then
    assertThat(target).contains("profile=:profile", "last_name=:lastName");
  }

  @Test
  void whenNoUniqueConstraintExists__AndAutoId__ShouldNotContainWhereClause() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityThree.class);
    // then
    assertThat(query).doesNotContain("WHERE");
  }

  @Test
  void whenIdAutoAndHasNoUniqueColumns__ShouldContainIdsInWhereClause() {
    Map<String, String> idMappings = Arrays
        .stream(JpaQueryTestEntityThree.class.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(Id.class))
        .collect(Collectors.toMap(
            field -> field.getAnnotation(Column.class).name(),
            Field::getName
        ));

    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityThree.class);

    // then
    idMappings.forEach((key, value) -> assertThat(query)
        .contains(key)
        .contains(value));
  }

  @Test
  void whenJoinColumn__ShouldContainCorrectFieldName() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityThree.class);

    // then
    assertThat(query)
        .contains("test_entity_id")
        .contains("testEntity");
  }

  @Test
  void whenLastModifiedExists__ShouldReflectSuchFieldAndColumn() {
    // when
    String query = builder.getUpsertQuery(Artist.class);

    // then
    assertThat(query).contains("last_modified_at=NOW()");
  }

  @Test
  void whenCreatedAtdExists__ShouldReflectSuchFieldAndColumn() {
    // when
    String query = builder.getUpsertQuery(Artist.class);

    // then
    assertThat(query).contains("created_at");
  }

  @Data
  @Table(name = "query_entity", uniqueConstraints = {})
  @EqualsAndHashCode(callSuper = true)
  private static final class JpaQueryTestEntity extends BaseTimeEntity {

    @Id
    @Column(name = "")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "other_name")
    private String otherName;
  }

  @Table(name = "jpa_query_test_entity_one", uniqueConstraints = {
      @UniqueConstraint(name = "entity_one_unique", columnNames = {"profile", "last_name"})
  })
  private static final class JpaQueryTestEntityOne extends BaseEntity {

    @Column(name = "profile")
    String profile;
    @Column(name = "last_name")
    String lastName;
    @Column(name = "hello")
    String hello;
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
  }

  @Table(name = "jpa_query_test_entity_two", uniqueConstraints = {
      @UniqueConstraint(name = "entity_two_unique_one", columnNames = {"profile", "last_name"}),
      @UniqueConstraint(name = "entity_two_unique_two", columnNames = {"last_name", "hello"})
  })
  private static final class JpaQueryTestEntityTwo extends BaseEntity {

    @Column(name = "profile")
    String profile;
    @Column(name = "last_name")
    String lastName;
    @Column(name = "hello")
    String hello;
    @Id
    @Column(name = "id")
    private Long id;
  }

  @Table(name = "jpa_query_test_entity_three")
  private static final class JpaQueryTestEntityThree extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @JoinColumn(name = "test_entity_id")
    private TestEntity testEntity;
  }
}