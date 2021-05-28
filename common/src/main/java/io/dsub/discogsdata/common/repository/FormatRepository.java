package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.release.ReleaseItemFormat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormatRepository extends JpaRepository<ReleaseItemFormat, Long> {

}
