package io.dsub.discogs.common.repository.release;

import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.release.ReleaseItem;
import io.dsub.discogs.common.entity.release.ReleaseItemCreditedArtist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseCreditedArtistRepository
    extends JpaRepository<ReleaseItemCreditedArtist, Long> {

  boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);

  boolean existsByArtistIdAndReleaseItemId(Long artistId, Long releaseItemId);

  List<ReleaseItemCreditedArtist> findAllByReleaseItemId(Long releaseItemId);
}
