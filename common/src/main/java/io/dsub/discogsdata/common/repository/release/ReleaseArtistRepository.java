package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.entity.release.ReleaseItemArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseArtistRepository extends JpaRepository<ReleaseItemArtist, Long> {
  boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);

  List<ReleaseItemArtist> findAllByReleaseItemId(Long releaseItemId);
}
