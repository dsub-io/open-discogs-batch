package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.label.Label;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseItemWork extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "release_item_id")
    private ReleaseItem releaseItem;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private Label label;

    private String name;
    private String job;
}
