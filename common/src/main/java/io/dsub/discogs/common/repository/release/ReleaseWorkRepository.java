package io.dsub.discogs.common.repository.release;

import io.dsub.discogs.common.entity.release.ReleaseItem;
import io.dsub.discogs.common.entity.label.Label;
import io.dsub.discogs.common.entity.release.ReleaseItemWork;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseWorkRepository extends JpaRepository<ReleaseItemWork, Long> {

  boolean existsByLabelAndReleaseItem(Label label, ReleaseItem releaseItem);
}
