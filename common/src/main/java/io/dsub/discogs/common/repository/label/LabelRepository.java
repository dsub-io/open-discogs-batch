package io.dsub.discogs.common.repository.label;

import io.dsub.discogs.common.entity.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {

}
