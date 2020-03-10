package io.dsub.dumpdbmgmt.service;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.repository.ArtistRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ArtistService {

    private ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    public Artist save(Artist artist) {
        return this.artistRepository.save(artist);
    }

    public Iterable<Artist> saveAll(Iterable<Artist> artists) {
        return this.artistRepository.saveAll(artists);
    }

    public Artist findById(Long id) {
        Optional<Artist> artist = this.artistRepository.findById(id);
        return artist.orElse(null);
    }

    public Boolean existsById(Long id) {
        return this.artistRepository.existsById(id);
    }
}
