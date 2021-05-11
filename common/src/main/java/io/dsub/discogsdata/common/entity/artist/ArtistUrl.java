package io.dsub.discogsdata.common.entity.artist;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"artist_id", "url"}))
public class ArtistUrl extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "artist_id")
  private Artist artist;

  @Column(length = 5000, name = "url")
  private String url;
}
