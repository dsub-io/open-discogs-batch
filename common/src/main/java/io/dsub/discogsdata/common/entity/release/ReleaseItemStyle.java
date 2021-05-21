package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Style;
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
@Table(name = "release_item_style", uniqueConstraints =
@UniqueConstraint(name = "unique_release_item_style", columnNames = {
    "release_item_id", "style"}))
public class ReleaseItemStyle extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  @ManyToOne
  private ReleaseItem releaseItem;

  @JoinColumn(name = "style", referencedColumnName = "name")
  @ManyToOne
  private Style style;
}
