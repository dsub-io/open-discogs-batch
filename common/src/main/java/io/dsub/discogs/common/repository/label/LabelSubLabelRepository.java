package io.dsub.discogs.common.repository.label;

import io.dsub.discogs.common.entity.label.Label;
import io.dsub.discogs.common.entity.label.LabelSubLabel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelSubLabelRepository extends JpaRepository<LabelSubLabel, Long> {

  boolean existsByParentAndSubLabel(Label parent, Label subLabel);

  boolean deleteAllByParent(Label parent);
}
