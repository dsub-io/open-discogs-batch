package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"release_item_id", "genre"}))
public class ReleaseItemGenre extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "release_item_id")
    @ManyToOne
    private ReleaseItem releaseItem;

    @JoinColumn(name = "genre")
    @ManyToOne
    private Genre genre;
}
