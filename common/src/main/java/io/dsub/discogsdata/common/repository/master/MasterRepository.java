package io.dsub.discogsdata.common.repository.master;

import io.dsub.discogsdata.common.entity.master.Master;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterRepository extends JpaRepository<Master, Long> {
}
