package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Entity
@With
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "master")
public class Master extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Column(name = "id", columnDefinition = "serial")
  @Id
  private Long id;

  @Column(name = "year")
  private short year;

  @Column(name = "title", length = 2000)
  private String title;

  @Column(name = "data_quality")
  private String dataQuality;
}
