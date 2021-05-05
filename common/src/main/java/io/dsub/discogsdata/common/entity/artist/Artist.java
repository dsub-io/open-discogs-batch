package io.dsub.discogsdata.common.entity.artist;

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
public class Artist extends BaseTimeEntity {
  @Id private Long id;

  @Column(length = 1000)
  private String name;

  @Column(length = 2000)
  private String realName;

  @Column(columnDefinition = "LONGTEXT")
  private String profile;

  private String dataQuality;
}
