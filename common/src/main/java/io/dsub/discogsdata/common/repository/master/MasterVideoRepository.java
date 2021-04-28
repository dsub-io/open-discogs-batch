package io.dsub.discogsdata.common.repository.master;

import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterVideoRepository extends JpaRepository<MasterVideo, Long> {
    boolean existsByDescriptionAndUrlAndTitleAndMaster(String description, String url, String title, Master master);
}
