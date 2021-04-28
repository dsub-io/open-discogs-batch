package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistGroupRepository extends JpaRepository<ArtistGroup, Long> {
    boolean existsByArtistAndGroup(Artist artist, Artist group);

    List<ArtistGroup> findAllByArtistId(Long artistId);
}