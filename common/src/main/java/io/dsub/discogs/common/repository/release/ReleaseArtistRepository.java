package io.dsub.discogs.common.repository.release;

import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.release.ReleaseItem;
import io.dsub.discogs.common.entity.release.ReleaseItemArtist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseArtistRepository extends JpaRepository<ReleaseItemArtist, Long> {

  boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);

  List<ReleaseItemArtist> findAllByReleaseItemId(Long releaseItemId);
}
