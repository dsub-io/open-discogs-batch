package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionRepository extends JpaRepository<Version, Long> {
  Version findTopByOrderByIdDesc();
}
