package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterSubItemsCommand;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.artist.entity.Artist;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.genre.entity.Genre;
import io.dsub.discogs.common.master.entity.*;
import io.dsub.discogs.common.style.entity.Style;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.*;

@RequiredArgsConstructor
public class MasterSubItemsProcessor implements ItemProcessor<MasterSubItemsCommand, Collection<BaseEntity>> {

    private final EntityIdRegistry idRegistry;
    @Override
    public Collection<BaseEntity> process(MasterSubItemsCommand command) {

        if (command.getId() == null || command.getId() < 1) {
            return null;
        }

        ReflectionUtil.normalizeStringFields(command);

        List<BaseEntity> items = new ArrayList<>();
        long masterId = command.getId();

        if (command.getArtists() != null) {
            command.getArtists().stream()
                    .filter(Objects::nonNull)
                    .map(MasterSubItemsCommand.Artist::getId)
                    .distinct()
                    .filter(this::isExistingArtist)
                    .map(artistId -> getMasterArtist(masterId, artistId))
                    .forEach(items::add);
        }

        if (command.getVideos() != null) {
            command.getVideos().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(video -> video.getUrl() != null && !video.getUrl().isBlank())
                    .map(video -> getMasterVideo(masterId, video))
                    .forEach(items::add);
        }

        if (command.getGenres() != null) {
            command.getGenres().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .distinct()
                    .filter(this::isExistingGenre)
                    .map(genre -> getMasterGenre(masterId, genre))
                    .forEach(items::add);
        }

        if (command.getStyles() != null) {
            command.getStyles().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .distinct()
                    .filter(this::isExistingStyle)
                    .map(style -> getMasterStyle(masterId, style))
                    .forEach(items::add);
        }

        return items.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isExistingArtist(long id) {
        return idRegistry.exists(ARTIST, id);
    }

    private boolean isExistingStyle(String name) {
        return idRegistry.exists(STYLE, name);
    }

    private boolean isExistingGenre(String name) {
        return idRegistry.exists(GENRE, name);
    }

    private MasterGenre getMasterGenre(long masterId, String genre) {
        return MasterGenre.builder()
                .master(getMaster(masterId))
                .genre(getGenre(genre))
                .build();
    }

    private MasterStyle getMasterStyle(long masterId, String style) {
        return MasterStyle.builder()
                .master(getMaster(masterId))
                .style(getStyle(style))
                .build();
    }

    private Genre getGenre(String genre) {
        return Genre.builder().name(genre).build();
    }

    private Style getStyle(String style) {
        return Style.builder().name(style).build();
    }

    private MasterVideo getMasterVideo(long masterId, MasterSubItemsCommand.Video video) {
        return MasterVideo.builder()
                .master(getMaster(masterId))
                .description(video.getDescription())
                .title(video.getTitle())
                .url(video.getUrl())
                .build();
    }

    private MasterArtist getMasterArtist(long masterId, long artistId) {
        return MasterArtist.builder()
                .master(getMaster(masterId))
                .artist(getArtist(artistId))
                .build();
    }

    private Artist getArtist(long id) {
        return Artist.builder().id(id).build();
    }

    private Master getMaster(long id) {
        return Master.builder().id(id).build();
    }
}
