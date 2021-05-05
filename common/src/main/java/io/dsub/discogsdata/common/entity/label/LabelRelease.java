package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "label_release",
    uniqueConstraints = @UniqueConstraint(columnNames = {"release_id", "label_id"}))
public class LabelRelease extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "release_id")
  private ReleaseItem releaseItem;

  @ManyToOne
  @JoinColumn(name = "label_id")
  private Label label;

  private String categoryNumber;
}
