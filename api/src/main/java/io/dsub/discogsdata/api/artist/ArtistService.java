package io.dsub.discogsdata.api.artist;

import io.dsub.discogsdata.api.exception.ArtistNotFoundException;

import java.util.List;

public interface ArtistService {
    ArtistDto getArtistById(long id) throws ArtistNotFoundException;

    List<ArtistDto> getArtists();

    List<ArtistDto> getArtistsByName(String name);
}
