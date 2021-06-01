package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Genre;
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
    name = "master_genre",
    uniqueConstraints = @UniqueConstraint(name = "uq_master_genre_master_id_genre", columnNames = {
        "master_id",
        "genre"}))
public class MasterGenre extends BaseTimeEntity {

  private static final Long SerialVersionUID = 1L;

  @Id
  @Column(name = "id", columnDefinition = "serial")
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @JoinColumn(name = "master_id", referencedColumnName = "id")
  @ManyToOne
  private Master master;

  @JoinColumn(name = "genre", referencedColumnName = "name")
  @ManyToOne
  private Genre genre;
}
