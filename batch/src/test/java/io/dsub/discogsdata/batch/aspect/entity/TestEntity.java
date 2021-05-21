package io.dsub.discogsdata.batch.aspect.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table
public class TestEntity {

  String name;
  @Id
  private Long id;
  @Column
  private String fieldWithColumnName;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
