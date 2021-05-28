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
@Table(name = "artist_member", uniqueConstraints = @UniqueConstraint(name = "unique_artist_member", columnNames = {
    "artist_id", "member_id"}))
public class ArtistMember extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id", columnDefinition = "serial", updatable = false, nullable = false)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "artist_id", referencedColumnName = "id")
  private Artist artist;

  @ManyToOne
  @JoinColumn(name = "member_id", referencedColumnName = "id")
  private Artist member;
}
