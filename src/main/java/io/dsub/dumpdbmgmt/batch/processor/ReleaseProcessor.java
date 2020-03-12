package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.entity.nested.*;
import io.dsub.dumpdbmgmt.util.DateParser;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@StepScope
@Slf4j
@Component("releaseProcessor")
public class ReleaseProcessor implements ItemProcessor<XmlRelease, Release> {

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
                release = release.withMasterRelease(xmlRelease.getMaster().getMasterId());
            }
        }

        if (xmlRelease.getAlbumArtists() != null) {
            for (XmlRelease.AlbumArtist source : xmlRelease.getAlbumArtists()) {
                release = release.withAddArtists(source.getId());
            }
        }

        if (xmlRelease.getLabels() != null) {
            for (XmlRelease.Label entry : xmlRelease.getLabels()) {
                release = release.withAddCatalogRefs(new CatalogRef(entry.getId(), entry.getCatno()));
            }
        }


        if (xmlRelease.getCompanies() != null) {
            for (XmlRelease.Company company : xmlRelease.getCompanies()) {
                release = release.withAddCompanies(new Company(company.getId(), company.getJob()));
            }
        }

        if (xmlRelease.getCreditedArtists() != null) {
            for (XmlRelease.CreditedArtist creditedArtist : xmlRelease.getCreditedArtists()) {
                release = release.withAddCreditedArtists(new CreditedArtist(creditedArtist.getId(), creditedArtist.getRole()));
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
}
