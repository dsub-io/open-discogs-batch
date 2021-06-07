package io.dsub.discogs.common.repository;

import io.dsub.discogs.common.entity.release.ReleaseItemTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<ReleaseItemTrack, Long> {

}
