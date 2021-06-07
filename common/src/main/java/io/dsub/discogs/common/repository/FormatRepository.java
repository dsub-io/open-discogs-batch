package io.dsub.discogs.common.repository;

import io.dsub.discogs.common.entity.release.ReleaseItemFormat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormatRepository extends JpaRepository<ReleaseItemFormat, Long> {

}
