package io.dsub.discogsdata.common.entity.artist;

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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;


@Data
@Entity
@With
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "artist_url", uniqueConstraints = @UniqueConstraint(name = "uq_artist_url_artist_id_url", columnNames = {
    "artist_id", "url"}))
public class ArtistUrl extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id", columnDefinition = "serial")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "artist_id", referencedColumnName = "id")
  private Artist artist;

  @Column(length = 5000, name = "url")
  private String url;
}