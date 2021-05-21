package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Genre;
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
@Table(name = "release_item_genre", uniqueConstraints = @UniqueConstraint(name = "unique_release_item_genre", columnNames = {
    "release_item_id", "genre"}))
public class ReleaseItemGenre extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  @ManyToOne
  private ReleaseItem releaseItem;

  @JoinColumn(name = "genre", referencedColumnName = "name")
  @ManyToOne
  private Genre genre;
}
