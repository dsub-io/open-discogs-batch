package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Master extends BaseEntity {
  @Id private Long id;

  private short year;

  @Column(length = 2000)
  private String title;

  private String dataQuality;

  @OneToOne private ReleaseItem mainReleaseItem;
}
