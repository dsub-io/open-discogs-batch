package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.release.ReleaseItemTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<ReleaseItemTrack, Long> {

}
