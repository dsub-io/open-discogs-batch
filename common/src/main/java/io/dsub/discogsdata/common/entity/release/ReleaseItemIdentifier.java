package io.dsub.discogsdata.common.entity.release;

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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = "release_item_identifier", uniqueConstraints = {
    @UniqueConstraint(name = "uq_identifier_type_description_value_release_item_id", columnNames = {
        "type", "description", "value", "release_item_id"})})
public class ReleaseItemIdentifier extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(name = "type")
  private String type;

  @Column(name = "description", length = 20000)
  private String description;

  @Column(name = "value")
  private String value;

  @ManyToOne
  @JoinColumn(name = "release_item_id", referencedColumnName = "id")
  private ReleaseItem releaseItem;
}
