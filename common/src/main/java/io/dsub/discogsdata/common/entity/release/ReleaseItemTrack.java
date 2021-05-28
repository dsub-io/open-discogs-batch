package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = "release_item_track", uniqueConstraints = {
    @UniqueConstraint(name = "unique_track", columnNames = {
        "name", "title", "duration", "release_item_id"
    })
})
public class ReleaseItemTrack extends BaseEntity {

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(name = "name", length = 2000)
  private String name;

  @Column(name = "title", length = 2000)
  private String title;

  @Column(name = "duration", length = 1000)
  private String duration;

  @ManyToOne
  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  private ReleaseItem releaseItem;
}
