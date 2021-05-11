package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"label_id", "url"}))
public class LabelUrl extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "label_id")
  private Label label;

  @Column(length = 5000, name = "url")
  private String url;
}
