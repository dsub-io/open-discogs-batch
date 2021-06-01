package io.dsub.discogsdata.batch.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.junit.jupiter.api.Test;

class MySQLJpaEntityQueryBuilderTest {

  MySQLJpaEntityQueryBuilder builder = new MySQLJpaEntityQueryBuilder();

  @Test
  void whenGetUpsertQuery__ShouldContainOnDuplicateKeyUpdateClause() {
    // when
    String query = builder.getUpsertQuery(Artist.class);

    // then
    assertThat(query).contains("ON DUPLICATE KEY UPDATE");
  }

  @Test
  void whenGetUpsertQuery__WithoutJoinColumns__ShouldNotIncludeSelectClause() {
    // when
    String query = builder.getUpsertQuery(Artist.class);

    // then
    assertThat(query).doesNotContain("SELECT 1 FROM", "WHERE");
  }

  @Test
  void whenGetUpsertQuery__WithJoinColumns__ShouldIncludeSelectClauses() {
    // when
    String query = builder.getUpsertQuery(ArtistAlias.class);

    // then
    assertAll(
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :artist"),
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :alias")
    );
  }

  @Test
  void whenGetUpsertQuery__WithIdAutoAndNoUniqueConstraints__ShouldEqualAsInsertQuery() {
    @Table(name = "test_entity")
    class TestEntity extends BaseEntity {

      @Id
      @Column(name = "id")
      @GeneratedValue
      Long id;
      @Column(name = "name")
      String name;
      @Column(name = "real_name")
      String realName;
    }

    // when
    String query = builder.getUpsertQuery(TestEntity.class);

    // then
    assertThat(query).isEqualTo(builder.getInsertQuery(TestEntity.class));
  }

  @Test
  void whenIdOnlyInsertQuery__ShouldOnlyIncludeIdMappings() {
    // when
    String query = builder.getIdOnlyInsertQuery(Artist.class);

    // then
    assertAll(
        () -> assertThat(query).contains("id", ":id"),
        () -> assertThat(query).contains("last_modified_at", "created_at", "NOW()"),
        () -> assertThat(query).doesNotContain(":lastModifiedAt", ":createdAt", ":name", "name"));
  }
}