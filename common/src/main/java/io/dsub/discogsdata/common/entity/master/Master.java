package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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

  @Column(name = "id")
  @Id
  private Long id;

  @Column(name = "year")
  private short year;

  @Column(name = "title", length = 2000)
  private String title;

  @Column(name = "data_quality")
  private String dataQuality;

  @OneToOne
  @JoinColumn(name = "master_main_release_item", unique = true, nullable = false)
  private ReleaseItem mainReleaseItem;
}
