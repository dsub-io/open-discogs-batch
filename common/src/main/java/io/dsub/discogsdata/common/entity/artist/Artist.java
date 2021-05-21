package io.dsub.discogsdata.common.entity.artist;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
@Table(name = "artist")
public class Artist extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Column(name = "id")
  @Id
  private Long id;

  @Column(name = "name", length = 1000)
  private String name;

  @Column(name = "real_name", length = 2000)
  private String realName;

  @Column(name = "profile", length = 40000)
  private String profile;

  @Column(name = "data_quality")
  private String dataQuality;
}
