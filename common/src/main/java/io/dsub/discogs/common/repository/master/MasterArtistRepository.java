package io.dsub.discogs.common.repository.master;

import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.master.Master;
import io.dsub.discogs.common.entity.master.MasterArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterArtistRepository extends JpaRepository<MasterArtist, Long> {

  boolean existsByMasterAndArtist(Master master, Artist artist);
}
