package io.dsub.discogsdata.artist;

import io.dsub.discogsdata.exception.ArtistNotFoundException;

import java.util.List;

public interface ArtistService {
    ArtistDto getArtistById(long id) throws ArtistNotFoundException;

    List<ArtistDto> getArtists();

    List<ArtistDto> getArtistsByName(String name);
}
