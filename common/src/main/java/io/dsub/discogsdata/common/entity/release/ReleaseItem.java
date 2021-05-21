package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Entity
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "release_item")
public class ReleaseItem extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Column(name = "id")
  @Id
  private Long id;

  @Column(name = "is_master")
  private boolean isMaster;

  @Column(name = "status")
  private String status;

  @Column(name = "title", length = 10000)
  private String title;

  @Column(name = "country")
  private String country;

  @Column(name = "notes", length = 40000)
  private String notes;

  @Column(name = "data_quality")
  private String dataQuality;

  @ManyToOne
  @JoinColumn(name = "master_id", referencedColumnName = "id")
  private Master master;

  @Column(name = "has_valid_month")
  private boolean hasValidMonth;

  @Column(name = "has_valid_day")
  private boolean hasValidDay;

  @Column(name = "has_valid_year")
  private boolean hasValidYear;

  @Column(name = "listed_release_date")
  private String listedReleaseDate;

  @Column(name = "release_date")
  private LocalDate releaseDate;

  public boolean getIsMaster() {
    return this.isMaster;
  }

  public Long getMasterId() {
    return master == null ? null : master.getId();
  }
}
