package io.dsub.discogsdata.common.entity.label;

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
    name = "label_sub_label",
    uniqueConstraints = @UniqueConstraint(name = "unique_label_sub_label", columnNames = {
        "parent_label_id", "sub_label_id"}))
public class LabelSubLabel extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;
  /*
   * Convenient READ_ONLY access for actually mapped class.
   * NOTE: mark any FetchType to avoid warning about immutability.
   */
  @ManyToOne
  @JoinColumn(name = "parent_label_id", referencedColumnName = "id")
  private Label parent;

  @ManyToOne
  @JoinColumn(name = "sub_label_id", referencedColumnName = "id")
  private Label subLabel;
}
