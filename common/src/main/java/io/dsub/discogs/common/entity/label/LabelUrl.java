package io.dsub.discogs.common.entity.label;

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
    name = "label_url",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_label_url_label_id_url",
            columnNames = {"label_id", "url"}))
public class LabelUrl extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "label_id", referencedColumnName = "id")
  private Label label;

  @Column(length = 5000, name = "url")
  private String url;
}
