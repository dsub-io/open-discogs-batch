package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.release.ReleaseItemIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentifierRepository extends JpaRepository<ReleaseItemIdentifier, Long> {

}
