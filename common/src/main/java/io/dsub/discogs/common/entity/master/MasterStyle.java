package io.dsub.discogs.common.entity.master;

import io.dsub.discogs.common.entity.Style;
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
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "master_style",
    uniqueConstraints = @UniqueConstraint(name = "uq_master_style_master_id_style", columnNames = {
        "master_id",
        "style"}))
public class MasterStyle extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "master_id", referencedColumnName = "id")
  private Master master;

  @ManyToOne
  @JoinColumn(name = "style", referencedColumnName = "name")
  private Style style;
}
