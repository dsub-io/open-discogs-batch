package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.release.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
