package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.entity.nested.Format;
import io.dsub.dumpdbmgmt.entity.nested.Identifier;
import io.dsub.dumpdbmgmt.entity.nested.Track;
import io.dsub.dumpdbmgmt.entity.nested.Video;
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

    DateParser dateParser;

    public ReleaseProcessor(DateParser dateParser) {
        this.dateParser = dateParser;
    }

    @Override
    public Release process(XmlRelease xmlRelease) {

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
