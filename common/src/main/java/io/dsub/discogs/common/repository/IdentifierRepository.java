package io.dsub.discogs.common.repository;

import io.dsub.discogs.common.entity.release.ReleaseItemIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentifierRepository extends JpaRepository<ReleaseItemIdentifier, Long> {

}
