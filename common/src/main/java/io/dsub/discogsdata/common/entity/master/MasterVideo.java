package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "master_video",
    uniqueConstraints = @UniqueConstraint(columnNames = {"master_id", "url"}))
public class MasterVideo extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 1000)
  private String title;

  @Column(length = 5000)
  private String description;

  @Column(length = 5000)
  private String url;

  @ManyToOne
  @JoinColumn(name = "master_id")
  private Master master;
}
