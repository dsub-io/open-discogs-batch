package io.dsub.discogsdata.common.entity.artist;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import javax.persistence.*;


@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"artist_id", "url"}))
public class ArtistUrl extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "artist_url_generator")
  @GenericGenerator(
          name = "artist_url_generator",
          strategy = "sequence",
          parameters = {
                  @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = SequenceStyleGenerator.DEF_SEQUENCE_NAME),
                  @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "1"),
                  @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1000"),
                  @Parameter(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled-lo")
          }
  )
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "artist_id")
  private Artist artist;

  @Column(length = 5000, name = "url")
  private String url;
}
