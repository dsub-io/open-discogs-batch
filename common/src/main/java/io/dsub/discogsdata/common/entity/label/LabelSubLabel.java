package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@Table(name = "label_sub_label",
        uniqueConstraints = @UniqueConstraint(columnNames = {"parent_label_id", "sub_label_id"}))
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor

public class LabelSubLabel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @ManyToOne
    @JoinColumn(name = "parent_label_id")
    private Label parent;

    @ManyToOne
    @JoinColumn(name = "sub_label_id")
    private Label subLabel;
}
