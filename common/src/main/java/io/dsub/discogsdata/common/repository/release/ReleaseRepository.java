package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseRepository extends JpaRepository<ReleaseItem, Long> {
}
