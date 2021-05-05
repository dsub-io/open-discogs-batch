package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseItemCreditedArtist extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "release_item_id")
  @ManyToOne
  private ReleaseItem releaseItem;

  @JoinColumn(name = "artist_id")
  @ManyToOne
  private Artist artist;

  @Column(columnDefinition = "TEXT")
  private String role;
}
