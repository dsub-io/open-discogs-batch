package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.service.ArtistService;
import io.dsub.dumpdbmgmt.xmlobj.XmlMaster;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component("masterReleaseProcessor")
public class MasterReleaseProcessor implements ItemProcessor<XmlMaster, MasterRelease> {

    private ArtistService artistService;

    public MasterReleaseProcessor(ArtistService artistService) {
        this.artistService = artistService;
    }


    /**
     * Relationship with artist must be established at this point,
     * to prepare release update step.
     *
     * @param item Objectified xml document entity
     * @return transformed entity.
     */
    @Override
    public MasterRelease process(XmlMaster item) {
        MasterRelease masterRelease = new MasterRelease(item.getId());

        if (item.getDataQuality() != null) {
            masterRelease = masterRelease.withDataQuality(item.getDataQuality());
        }

        if (item.getYear() != null) {
            masterRelease = masterRelease.withReleaseYear(item.getYear());
        }

        if (item.getTitle() != null) {
            masterRelease = masterRelease.withTitle(item.getTitle());
        }

        if (item.getStyles() != null) {
            masterRelease = masterRelease.withStyles(item.getStyles());
        }

        if (item.getGenres() != null) {
            masterRelease = masterRelease.withGenres(item.getGenres());
        }

        if (item.getArtists() != null) {
            for (XmlMaster.ArtistInfo source : item.getArtists()) {
                Artist artist = artistService.findById(source.getId());
                if (artist != null) {
                    artist = artist.withAddMasterReleases(masterRelease);
                    artistService.save(artist);
                    masterRelease = masterRelease.withAddArtists(artist);
                }
            }
        }

        return masterRelease;
    }
}
