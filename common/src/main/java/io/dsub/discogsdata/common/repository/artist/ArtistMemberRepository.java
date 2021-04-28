package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistMemberRepository extends JpaRepository<ArtistMember, Long> {
    boolean existsByArtistAndMember(Artist artist, Artist member);

    List<ArtistMember> findAllByArtistId(Long artistId);
}
