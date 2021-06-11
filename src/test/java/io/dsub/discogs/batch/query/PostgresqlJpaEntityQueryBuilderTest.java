package io.dsub.discogs.batch.query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.dsub.discogs.batch.query.JpaEntityExtractorTest.TestEntity;
import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.artist.ArtistMember;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.base.BaseTimeEntity;
import io.dsub.discogs.common.entity.release.ReleaseItemTrack;
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

  PostgresqlJpaEntityQueryBuilder builder = new PostgresqlJpaEntityQueryBuilder();

  @Test
  void ifUniqueConstraintExistsTwice__AndAutoId__ShouldContainIdsAndUniqueConstraints() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityTwo.class);

    int startLen = "ON CONFLICT (".length();
    int startIdx = query.indexOf("ON CONFLICT (");
    int lastIdx = query.indexOf(") DO UPDATE");
    String[] parts = query.substring(startIdx + startLen, lastIdx).split(",");

    // then
    assertThat(parts).contains("profile", "last_name", "hello", "id");
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
    Map<String, String> idMappings =
        Arrays.stream(JpaQueryTestEntityThree.class.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Id.class))
            .collect(
                Collectors.toMap(
                    field -> field.getAnnotation(Column.class).name(), Field::getName));

    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityThree.class);

    // then
    idMappings.forEach((key, value) -> assertThat(query).contains(key).contains(value));
  }

  @Test
  void whenJoinColumn__ShouldContainCorrectFieldName() {
    // when
    String query = builder.getUpsertQuery(JpaQueryTestEntityThree.class);

    // then
    assertThat(query).contains("test_entity_id").contains("testEntity");
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

  @Test
  void whenGetUpsertQuery__WithEntityWithJoinColumns__ShouldIncludeWhereClause() {
    // when
    String query = builder.getUpsertQuery(ArtistMember.class);

    // then
    assertAll(
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :artist"),
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :member"));
  }

  @Test
  void whenGetIdOnlyInsertQuery__ShouldOnlyIncludeIdFields() {
    // when
    String query = builder.getIdOnlyInsertQuery(ArtistMember.class);

    // then
    assertAll(
        () -> assertThat(query).doesNotContain(":artist"),
        () -> assertThat(query).doesNotContain(":member"),
        () -> assertThat(query).contains("ON CONFLICT DO NOTHING"));
  }

  @Test
  void test() {
    String query = builder.getUpsertQuery(ReleaseItemTrack.class);
    System.out.println(query);
  }

  @Data
  @Table(
      name = "query_entity",
      uniqueConstraints = {})
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

  @Table(
      name = "jpa_query_test_entity_one",
      uniqueConstraints = {
        @UniqueConstraint(
            name = "entity_one_unique",
            columnNames = {"profile", "last_name"})
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

  @Table(
      name = "jpa_query_test_entity_two",
      uniqueConstraints = {
        @UniqueConstraint(
            name = "entity_two_unique_one",
            columnNames = {"profile", "last_name"}),
        @UniqueConstraint(
            name = "entity_two_unique_two",
            columnNames = {"last_name", "hello"})
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
