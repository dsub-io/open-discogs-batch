package io.dsub.discogs.common.repository.master;

import io.dsub.discogs.common.entity.master.MasterGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterGenreRepository extends JpaRepository<MasterGenre, Long> {

}
