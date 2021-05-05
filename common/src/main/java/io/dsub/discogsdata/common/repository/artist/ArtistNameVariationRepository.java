package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.ArtistNameVariation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistNameVariationRepository extends JpaRepository<ArtistNameVariation, Long> {
    boolean existsByArtistIdAndName(Long artistId, String name);

    List<ArtistNameVariation> findAllByArtistId(Long artistId);
}
