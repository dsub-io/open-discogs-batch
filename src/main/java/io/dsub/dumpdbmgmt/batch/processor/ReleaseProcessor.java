package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.entity.intermed.ArtistCredit;
import io.dsub.dumpdbmgmt.entity.intermed.CompanyRelease;
import io.dsub.dumpdbmgmt.entity.intermed.LabelRelease;
import io.dsub.dumpdbmgmt.entity.nested.Format;
import io.dsub.dumpdbmgmt.entity.nested.Identifier;
import io.dsub.dumpdbmgmt.entity.nested.Track;
import io.dsub.dumpdbmgmt.entity.nested.Video;
import io.dsub.dumpdbmgmt.service.*;
import io.dsub.dumpdbmgmt.util.DateParser;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@StepScope
@Slf4j
@Component("releaseProcessor")
public class ReleaseProcessor implements ItemProcessor<XmlRelease, Release> {

    MasterReleaseService masterReleaseService;
    LabelService labelService;
    ArtistService artistService;
    ReleaseService releaseService;
    ArtistCreditService artistCreditService;
    LabelReleaseService labelReleaseService;
    CompanyReleaseService companyReleaseService;
    DateParser dateParser;

    public ReleaseProcessor(MasterReleaseService masterReleaseService, LabelService labelService, ArtistService artistService, ReleaseService releaseService, ArtistCreditService artistCreditService, LabelReleaseService labelReleaseService, CompanyReleaseService companyReleaseService, DateParser dateParser) {
        this.masterReleaseService = masterReleaseService;
        this.labelService = labelService;
        this.artistService = artistService;
        this.releaseService = releaseService;
        this.artistCreditService = artistCreditService;
        this.labelReleaseService = labelReleaseService;
        this.companyReleaseService = companyReleaseService;
        this.dateParser = dateParser;
    }

    @Override
    public Release process(XmlRelease xmlRelease) {

        if (xmlRelease.getReleaseId() == null) {
            return null;
        }

        Release release = new Release(xmlRelease.getReleaseId());

        // Insert "Unknown" as value if an xml does not contain corresponding element.
        String unknown = "Unknown";

        if (xmlRelease.getNotes() != null) {
            release = release.withNotes(xmlRelease.getNotes());
        } else {
            release = release.withNotes(unknown);
        }
        if (xmlRelease.getStatus() != null) {
            release = release.withStatus(xmlRelease.getStatus());
        } else {
            release = release.withStatus(unknown);
        }
        if (xmlRelease.getCountry() != null) {
            release = release.withCountry(xmlRelease.getCountry());
        } else {
            release = release.withCountry(unknown);
        }
        if (xmlRelease.getTitle() != null) {
            release = release.withTitle(xmlRelease.getTitle());
        } else {
            release = release.withTitle(unknown);
        }
        if (xmlRelease.getDataQuality() != null) {
            release = release.withDataQuality(xmlRelease.getDataQuality());
        } else {
            release = release.withDataQuality(unknown);
        }

        // Check for null date
        if (xmlRelease.getReleaseDate() != null) {
            // Attempt parse the raw string into Date format.
            // If the string is malformed, the result will be Jan 01 1500.
            // If only the year is valid, the result will be Jun 06, YYYY.
            // If only year and month is valid, the result will be MM 06, YYYY.
            LocalDate date = DateParser.parse(xmlRelease.getReleaseDate());
            release = release.withReleaseDate(date);
            release = release.withViewDate(xmlRelease.getReleaseDate());
        } else {
            release = release.withReleaseDate(LocalDate.of(1500, 1, 1));
            release = release.withViewDate("Unknown");
        }

        if (xmlRelease.getMaster() != null) {
            boolean isMaster = false;
            try {
                isMaster = xmlRelease.getMaster().isMaster();
            } catch (Exception ignored) {
            }
            release = release.withIsMain(isMaster);
        } else {
            release = release.withIsMain(false);
        }

        if (xmlRelease.getIdentifiers() != null) {
            for (XmlRelease.Identifier source : xmlRelease.getIdentifiers()) {
                Identifier identifier = makeIdentifier(source);
                release = release.withAddIdentifiers(identifier);
            }
        }

        if (xmlRelease.getVideos() != null) {
            for (XmlRelease.Video source : xmlRelease.getVideos()) {
                Video video = makeVideo(source);
                release = release.withAddVideos(video);
            }
        }

        if (xmlRelease.getFormats() != null) {
            for (XmlRelease.Format source : xmlRelease.getFormats()) {
                Format format = makeFormat(source);
                release = release.withAddFormats(format);
            }
        }

        if (xmlRelease.getTracks() != null) {
            for (XmlRelease.Track source : xmlRelease.getTracks()) {
                Track track = makeTrack(source);
                release = release.withAddTracks(track);
            }
        }

        if (xmlRelease.getMaster() != null) {
            if (xmlRelease.getMaster().getMasterId() != null) {
                this.addReleaseToMaster(xmlRelease.getMaster().getMasterId(), release.getId());
                release = release.withMasterRelease(xmlRelease.getMaster().getMasterId());
            }
        }

        if (xmlRelease.getAlbumArtists() != null) {
            for (XmlRelease.AlbumArtist source : xmlRelease.getAlbumArtists()) {
                addReleaseToArtist(source.getId(), release.getId());
                release = release.withAddArtists(source.getId());
            }
        }

        if (xmlRelease.getLabels() != null) {
            for (XmlRelease.Label entry : xmlRelease.getLabels()) {
                ObjectId id = makeLabelRelease(entry.getCatno(), entry.getId(), release.getId());
                if (id != null) {
                    release = release.withAddLabelReleases(id);
                }
            }
        }


        if (xmlRelease.getCompanies() != null) {
            for (XmlRelease.Company company : xmlRelease.getCompanies()) {
                ObjectId id = makeCompanyRelease(company.getJob(), company.getId(), release.getId());
                if (id != null) {
                    release = release.withAddCompaniesReleases(id);
                }
            }
        }

        if (xmlRelease.getCreditedArtists() != null) {
            for (XmlRelease.CreditedArtist creditedArtist : xmlRelease.getCreditedArtists()) {
                ObjectId id = makeCreditedArtist(creditedArtist.getRole(), creditedArtist.getId(), release.getId());
                if (id != null) {
                    release = release.withAddCreditedArtists(id);
                }
            }
        }

        return release;
    }

    private Format makeFormat(XmlRelease.Format source) {
        Format format = new Format();
        if (source.getName() != null) {
            format = format.withName(source.getName());
        }
        if (source.getQty() != null) {
            format = format.withQty(source.getQty().shortValue());
        }
        if (source.getDescription() != null) {
            for (String s : source.getDescription()) {
                Set<String> descriptions = format.getDescriptions();
                descriptions.add(s);
                format = format.withDescriptions(descriptions);
            }
        }
        return format;
    }

    private Identifier makeIdentifier(XmlRelease.Identifier source) {
        Identifier identifier = new Identifier();
        if (source.getDescription() != null) {
            identifier = new Identifier(source.getDescription());
        }
        if (source.getType() != null) {
            identifier = identifier.withType(source.getType());
        }
        if (source.getValue() != null) {
            identifier = identifier.withValue(source.getValue());
        }
        return identifier;
    }

    private Video makeVideo(XmlRelease.Video source) {
        Video video = new Video();
        if (source.getTitle() != null) {
            video = video.withTitle(source.getTitle());
        }
        if (source.getDescription() != null) {
            video = video.withDescription(source.getDescription());
        }
        if (source.getUrl() != null) {
            video = video.withUrl(source.getUrl());
        }
        return video;
    }

    private Track makeTrack(XmlRelease.Track source) {
        Track track = new Track();
        if (source.getTitle() != null) {
            track = track.withTitle(source.getTitle());
        }
        if (source.getPosition() != null) {
            track = track.withPosition(source.getPosition());
        }
        if (source.getDuration() != null) {
            track = track.withDuration(source.getDuration());
        }
        return track;
    }

//    @Synchronized
    private void addReleaseToMaster(Long masterId, Long releaseId) {
        MasterRelease masterRelease = masterReleaseService.findById(masterId);
        if (masterRelease != null) {
            masterRelease = masterRelease.withAddReleases(releaseId);
            masterReleaseService.save(masterRelease);
        }
    }

//    @Synchronized
    private void addReleaseToArtist(Long artistId, Long releaseId) {
        Artist artist = artistService.findById(artistId);
        if (artist != null) {
            artist = artist.withAddReleases(releaseId);
            artistService.save(artist);
        }
    }

//    @Synchronized
    private ObjectId makeLabelRelease(String catNo, Long labelId, Long releaseId) {
        Label label = labelService.findById(labelId);

        if (label != null) {
            LabelRelease labelRelease = new LabelRelease();
            labelRelease = labelRelease.withCatNo(catNo);
            labelRelease = labelRelease.withLabel(labelId);
            labelRelease = labelRelease.withRelease(releaseId);
            labelRelease = labelReleaseService.save(labelRelease);

            label = label.withAddLabelRelease(labelRelease.getId());
            labelService.save(label);
            return labelRelease.getId();
        }
        return null;
    }

//    @Synchronized
    private ObjectId makeCompanyRelease(String serviceNote, Long labelId, Long releaseId) {
        Label label = labelService.findById(labelId);

        if (label != null) {
            CompanyRelease companyRelease = new CompanyRelease();
            companyRelease = companyRelease.withServiceNote(serviceNote);
            companyRelease = companyRelease.withLabel(label.getId());
            companyRelease = companyRelease.withRelease(releaseId);
            companyRelease = companyReleaseService.save(companyRelease);

            label = label.withAddCompanyReleases(companyRelease.getId());
            labelService.save(label);
            return companyRelease.getId();
        }

        return null;
    }

//    @Synchronized
    private ObjectId makeCreditedArtist(String role, Long artistId, Long releaseId) {
        Artist artist = artistService.findById(artistId);

        if (artist != null) {
            ArtistCredit artistCredit = new ArtistCredit();
            artistCredit = artistCredit.withCredit(role);
            artistCredit = artistCredit.withArtist(artistId);
            artistCredit = artistCredit.withRelease(releaseId);
            artistCredit = artistCreditService.save(artistCredit);

            artist = artist.withAddCredits(artistCredit.getId());
            artistService.save(artist);
            return artistCredit.getId();
        }

        return null;
    }
}
