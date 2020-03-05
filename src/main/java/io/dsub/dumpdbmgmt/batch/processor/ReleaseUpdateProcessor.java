package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.entity.intermed.ArtistCredit;
import io.dsub.dumpdbmgmt.entity.intermed.LabelRelease;
import io.dsub.dumpdbmgmt.entity.intermed.WorkRelease;
import io.dsub.dumpdbmgmt.service.*;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component("releaseUpdateProcessor")
public class ReleaseUpdateProcessor implements ItemProcessor<XmlRelease, Release> {

    MasterReleaseService masterReleaseService;
    LabelService labelService;
    ArtistService artistService;
    ReleaseService releaseService;
    ArtistCreditService artistCreditService;
    LabelReleaseService labelReleaseService;
    WorkReleaseService workReleaseService;

    public ReleaseUpdateProcessor(MasterReleaseService masterReleaseService, LabelService labelService, ArtistService artistService, ReleaseService releaseService, ArtistCreditService artistCreditService, LabelReleaseService labelReleaseService, WorkReleaseService workReleaseService) {
        this.masterReleaseService = masterReleaseService;
        this.labelService = labelService;
        this.artistService = artistService;
        this.releaseService = releaseService;
        this.artistCreditService = artistCreditService;
        this.labelReleaseService = labelReleaseService;
        this.workReleaseService = workReleaseService;
    }

    /**
     * Due to the assignment for entity id generation, it is necessary to mention that
     * intermediate relational object must be persisted before get assigned to be a
     * part of relationship.
     * <p>
     * Otherwise, it may return an exception telling : Cannot reference object with NULL ID value.
     *
     * @param item objectified xml document entity.
     * @return transformed entity.
     */

    @Override
    public Release process(XmlRelease item) {

        Release release = releaseService.findById(item.getReleaseId());
        if (release == null) {
            return null;
        }

        if (item.getMaster() != null) {
            if (item.getMaster().getMasterId() != null) {
                MasterRelease masterRelease = masterReleaseService.findById(item.getMaster().getMasterId());
                if (masterRelease != null) {
                    masterRelease = masterRelease.withAddReleases(release);
                    release = release.withMasterRelease(masterRelease);
                    masterReleaseService.save(masterRelease);
                }
            }
        }

        if (item.getAlbumArtists() != null) {
            for (XmlRelease.AlbumArtist source : item.getAlbumArtists()) {
                Artist artist = artistService.findById(source.getId());
                if (artist != null) {
                    artist = artist.withAddReleases(release);
                    release = release.withAddArtists(artist);
                    artistService.save(artist);
                }
            }
        }

        if (item.getLabels() != null) {
            for (XmlRelease.Label entry : item.getLabels()) {
                Label label = labelService.findById(entry.getId());
                if (label != null) {

                    String catNo = entry.getCatno();
                    LabelRelease labelRelease = new LabelRelease();
                    labelRelease = labelRelease.withCatNo(catNo);
                    labelRelease = labelRelease.withLabel(label);
                    labelRelease = labelRelease.withRelease(release);
                    labelRelease = labelReleaseService.save(labelRelease);

                    label = label.withAddLabelRelease(labelRelease);
                    release = release.withAddLabelReleases(labelRelease);
                    labelService.save(label);
                }
            }
        }


        if (item.getCompanies() != null) {
            for (XmlRelease.Company company : item.getCompanies()) {
                Label label = labelService.findById(company.getId());
                if (label != null) {
                    String serviceNote = company.getJob();

                    WorkRelease workRelease = new WorkRelease();
                    workRelease = workRelease.withServiceNote(serviceNote);
                    workRelease = workRelease.withLabel(label);
                    workRelease = workRelease.withRelease(release);
                    workRelease = workReleaseService.save(workRelease);
                    label = label.withAddWorkReleases(workRelease);
                    release = release.withAddWorkReleases(workRelease);
                    labelService.save(label);
                }
            }
        }

        if (item.getCreditedArtists() != null) {
            for (XmlRelease.CreditedArtist creditedArtist : item.getCreditedArtists()) {
                Artist artist = artistService.findById(creditedArtist.getId());
                if (artist != null) {
                    String role = creditedArtist.getRole();

                    ArtistCredit artistCredit = new ArtistCredit();
                    artistCredit = artistCredit.withArtist(artist);
                    artistCredit = artistCredit.withCredit(role);
                    artistCredit = artistCredit.withRelease(release);
                    artistCredit = artistCreditService.save(artistCredit);
                    artist = artist.withAddCredits(artistCredit);
                    release = release.withAddCreditedArtists(artistCredit);
                    artistService.save(artist);
                }
            }
        }

        return release;
    }
}

