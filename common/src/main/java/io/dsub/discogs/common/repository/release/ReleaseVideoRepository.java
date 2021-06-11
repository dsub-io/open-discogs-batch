package io.dsub.discogs.common.repository.release;

import io.dsub.discogs.common.entity.release.ReleaseItemVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseVideoRepository extends JpaRepository<ReleaseItemVideo, Long> {}
