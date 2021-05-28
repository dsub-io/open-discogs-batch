package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "master_video",
    uniqueConstraints = @UniqueConstraint(name = "unique_master_video", columnNames = {"master_id",
        "url"}))
public class MasterVideo extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(name = "title", length = 2000)
  private String title;

  @Column(name = "description", length = 40000)
  private String description;

  @Column(name = "url", length = 5000)
  private String url;

  @ManyToOne
  @JoinColumn(name = "master_id", referencedColumnName = "id")
  private Master master;
}
