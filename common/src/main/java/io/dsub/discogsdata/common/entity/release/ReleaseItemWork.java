package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.label.Label;
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
@Table(name = "release_item_work", uniqueConstraints = {
    @UniqueConstraint(name = "uq_release_item_work_release_item_id_label_id_work", columnNames = {
        "release_item_id", "label_id", "work"
    })
})
public class ReleaseItemWork extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  private ReleaseItem releaseItem;

  @ManyToOne
  @JoinColumn(name = "label_id", referencedColumnName = "id")
  private Label label;

  @Column(name = "work")
  private String work;
}
