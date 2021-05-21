package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.entity.release.ReleaseItemWork;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseWorkRepository extends JpaRepository<ReleaseItemWork, Long> {

  boolean existsByLabelAndReleaseItem(Label label, ReleaseItem releaseItem);
}
