package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.nested.CreditedRelease;
import io.dsub.dumpdbmgmt.service.ArtistService;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
public class ArtistReferenceProcessor implements ItemProcessor<XmlRelease, Set<Artist>> {

    private final ArtistService artistService;

    public ArtistReferenceProcessor(ArtistService artistService) {
        this.artistService = artistService;
    }

    @Override
    public Set<Artist> process(XmlRelease item) {
        Set<Artist> artistSet = Collections.synchronizedSet(new HashSet<>());

        item.getCreditedArtists().forEach(entry -> {
            Artist artist = findArtist(entry.getId(), artistSet);
            if (artist != null) {
                CreditedRelease cr = new CreditedRelease(item.getReleaseId(), entry.getRole());
                artistSet.add(artist.withAddCredits(cr));
            }
        });

        item.getAlbumArtists().forEach(entry -> {
            Artist artist = findArtist(entry.getId(), artistSet);
            if (artist != null) {
                artist = artist.withAddReleases(item.getReleaseId());
                artistSet.add(artist);
            }
        });

        return artistSet;
    }

    private Artist findArtist(Long id, Set<Artist> artistSet) {
        for (Artist artist : artistSet) {
            if (artist.getId().equals(id)) {
                artistSet.remove(artist);
                return artist;
            }
        }
        return artistService.findById(id);
    }
}
