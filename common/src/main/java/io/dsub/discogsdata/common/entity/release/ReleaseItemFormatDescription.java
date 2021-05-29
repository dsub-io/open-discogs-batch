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
@Table(name = "release_item_format_description",
    uniqueConstraints = @UniqueConstraint(name = "unique_format_description", columnNames = {
        "format_id", "description"}))
public class ReleaseItemFormatDescription extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "format_id", referencedColumnName = "id")
  private ReleaseItemFormat format;

  @Column(name = "description", length = 5000)
  private String description;
}
