package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.entity.release.ReleaseItemCreditedArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseCreditedArtistRepository extends JpaRepository<ReleaseItemCreditedArtist, Long> {
    boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);

    boolean existsByArtistIdAndReleaseItemId(Long artistId, Long releaseItemId);

    List<ReleaseItemCreditedArtist> findAllByReleaseItemId(Long releaseItemId);
}
