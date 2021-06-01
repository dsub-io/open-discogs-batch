package io.dsub.discogsdata.batch.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import org.junit.jupiter.api.Test;

class SqlJpaEntityQueryBuilderTest {

  SqlJpaEntityQueryBuilder<BaseEntity> queryBuilder =
      new SqlJpaEntityQueryBuilder<>() {
        @Override
        public String getUpsertQuery(Class<? extends BaseEntity> targetClass) {
          return null;
        }

        @Override
        public String getIdOnlyInsertQuery(Class<? extends BaseEntity> targetClass) {
          return null;
        }
      };

  @Test
  void whenGetInsertQuery__ShouldIncludeAllFields__ExceptValuesForDates() {
    // when
    String query = queryBuilder.getInsertQuery(Artist.class);

    // then
    assertThat(query)
        .contains("id", "created_at", "last_modified_at", "profile", "data_quality", "name", "real_name")
        .contains(":profile", ":dataQuality", ":name", ":realName")
        .doesNotContain(":createdAt", ":lastModifiedAt");
  }

  @Test
  void whenGetInsertQuery__ShouldIncludeAllJoinColumns() {
    // when
    String query = queryBuilder.getInsertQuery(ArtistMember.class);

    // then
    assertAll(
        () -> assertThat(query).contains(":member", ":artist"),
        () -> assertThat(query).doesNotContain(":createdAt", ":lastModifiedAt"),
        () -> assertThat(query).doesNotContain(":id")
    );
  }

  @Test
  void getRelationExistCountingWhereClause__ShouldIncludeAllJoinColumns() {
    // when
    String query = queryBuilder.getRelationExistCountingWhereClause(ArtistGroup.class);

    // then
    assertAll(
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :group"),
        () -> assertThat(query).contains("SELECT 1 FROM artist WHERE id = :artist")
    );
  }
}