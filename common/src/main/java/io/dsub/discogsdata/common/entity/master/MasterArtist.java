package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.artist.Artist;
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
    name = "master_artist",
    uniqueConstraints =
    @UniqueConstraint(
        name = "unique_master_artist",
        columnNames = {"master_id", "artist_id"}))
public class MasterArtist extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @JoinColumn(name = "master_id", referencedColumnName = "id")
  @ManyToOne
  private Master master;

  @JoinColumn(name = "artist_id", referencedColumnName = "id")
  @ManyToOne
  private Artist artist;
}
