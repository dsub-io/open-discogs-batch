package io.dsub.discogsdata.batch.aspect.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TestEntity {
  @Id private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
