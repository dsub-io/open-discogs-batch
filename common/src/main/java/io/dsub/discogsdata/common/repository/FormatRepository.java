package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.release.Format;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormatRepository extends JpaRepository<Format, Long> {
}
