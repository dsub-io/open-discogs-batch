package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistMemberRepository extends JpaRepository<ArtistMember, Long> {

  boolean existsByArtistAndMember(Artist artist, Artist member);

  List<ArtistMember> findAllByArtistId(Long artistId);
}
