package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "qty", "text", "release_item_id"}))
public class Format extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private Integer qty;

  @Column(length = 5000)
  private String text;

  @ElementCollection @Builder.Default private Set<String> description = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "release_item_id")
  private ReleaseItem releaseItem;
}
