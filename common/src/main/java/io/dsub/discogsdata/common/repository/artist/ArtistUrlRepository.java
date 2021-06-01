package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.ArtistUrl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistUrlRepository extends JpaRepository<ArtistUrl, Long> {

  boolean existsByArtistIdAndUrl(Long artistId, String url);

  List<ArtistUrl> findAllByArtistId(Long artistId);
}
