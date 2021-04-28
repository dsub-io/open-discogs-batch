package io.dsub.discogsdata.common.repository.master;

import io.dsub.discogsdata.common.entity.master.MasterGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterGenreRepository extends JpaRepository<MasterGenre, Long> {
}
