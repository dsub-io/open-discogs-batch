package io.dsub.discogs.common.entity.release;

import io.dsub.discogs.common.entity.base.BaseTimeEntity;
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
@Table(name = "release_item_video", uniqueConstraints =
@UniqueConstraint(name = "uq_release_item_video_release_item_id_url", columnNames = {
    "release_item_id", "url"}))
public class ReleaseItemVideo extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(name = "title", length = 10000)
  private String title;

  @Column(name = "description", length = 10000)
  private String description;

  @Column(name = "url", length = 10000)
  private String url;

  @ManyToOne
  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  private ReleaseItem releaseItem;
}
