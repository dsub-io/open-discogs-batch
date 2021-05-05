package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseItemVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseVideoRepository extends JpaRepository<ReleaseItemVideo, Long> {}
