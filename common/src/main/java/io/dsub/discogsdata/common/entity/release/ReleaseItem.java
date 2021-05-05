package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@AllArgsConstructor
public class ReleaseItem extends BaseEntity {
  @Id private Long id;

  private boolean isMaster;

  private String status;

  @Column(length = 5000)
  private String title;

  private String country;

  @Column(columnDefinition = "LONGTEXT")
  private String notes;

  private String dataQuality;

  @ManyToOne
  @JoinColumn(name = "master_id")
  private Master master;

  private boolean hasValidMonth;

  private boolean hasValidDay;

  private boolean hasValidYear;

  private String listedReleaseDate;

  private LocalDate releaseDate;

  public boolean getIsMaster() {
    return this.isMaster;
  }

  public Long getMasterId() {
    return master == null ? null : master.getId();
  }
}
