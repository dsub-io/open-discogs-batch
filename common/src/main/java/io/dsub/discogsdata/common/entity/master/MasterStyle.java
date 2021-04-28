package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "master_style", uniqueConstraints = @UniqueConstraint(columnNames = {"master_id", "style"}))
public class MasterStyle extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "master_id")
    @ManyToOne
    private Master master;

    @JoinColumn(name = "style")
    @ManyToOne
    private Style style;
}
