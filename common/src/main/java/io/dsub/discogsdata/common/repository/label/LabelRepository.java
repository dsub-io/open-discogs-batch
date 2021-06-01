package io.dsub.discogsdata.common.repository.label;

import io.dsub.discogsdata.common.entity.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {

}
