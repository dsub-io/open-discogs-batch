package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "master_artist", uniqueConstraints = @UniqueConstraint(columnNames = {"master_id", "artist_id"}))
public class MasterArtist extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "master_id",  unique = false)
    @ManyToOne
    private Master master;

    @JoinColumn(name = "artist_id", unique = false)
    @ManyToOne
    private Artist artist;
}
