package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Identifier extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String value;

    @ManyToOne
    @JoinColumn(name = "release_item_id")
    private ReleaseItem releaseItem;
}
