package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Label extends BaseTimeEntity {
  @Id private Long id;

  private String name;

  @Column(columnDefinition = "LONGTEXT")
  private String contactInfo;

  @Column(columnDefinition = "LONGTEXT")
  private String profile;

  private String dataQuality;
}
