package io.dsub.discogsdata.common.repository.master;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterArtistRepository extends JpaRepository<MasterArtist, Long> {
    boolean existsByMasterAndArtist(Master master, Artist artist);
}
