package io.dsub.discogs.api.artist;

import io.dsub.discogs.api.exception.ArtistNotFoundException;
import java.util.List;

public interface ArtistService {

  ArtistDto getArtistById(long id) throws ArtistNotFoundException;

  List<ArtistDto> getArtists();

  List<ArtistDto> getArtistsByName(String name);
}
