package io.dsub.discogs.common.repository.artist;

import io.dsub.discogs.common.entity.artist.ArtistNameVariation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistNameVariationRepository extends JpaRepository<ArtistNameVariation, Long> {

  boolean existsByArtistIdAndName(Long artistId, String name);

  List<ArtistNameVariation> findAllByArtistId(Long artistId);
}
