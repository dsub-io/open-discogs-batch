package io.dsub.discogs.batch.query;

import io.dsub.discogs.common.entity.base.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.junit.jupiter.api.Test;

class PostgresqlJpaEntityQueryBuilderTest {

  PostgresqlJpaEntityQueryBuilder builder = new PostgresqlJpaEntityQueryBuilder();


  @Test
  void givenEntityHasTransientField__whenGetSelectInsertQuery__ShouldExcludeIt() {
    @Table(name = "test_entity")
    class TestEntity extends BaseTimeEntity {
      @Column
      String name;
      @Column
      @Transient
      String realName;
    }
    // when
    String query = builder.getSelectInsertQuery(TestEntity.class);

    // then
    System.out.println(query);
  }
}
