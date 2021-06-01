package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

  boolean existsById(Long id);

  List<Artist> findAllByNameContains(String name);
}
