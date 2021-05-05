package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {
  boolean existsByArtistAndAlias(Artist artist, Artist alias);

  List<ArtistAlias> findAllByArtistId(Long artistId);
}
