package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand.AlbumArtist;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand.Format;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.artist.entity.Artist;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.genre.entity.Genre;
import io.dsub.discogs.common.label.entity.Label;
import io.dsub.discogs.common.label.entity.LabelRelease;
import io.dsub.discogs.common.master.entity.Master;
import io.dsub.discogs.common.release.entity.*;
import io.dsub.discogs.common.style.entity.Style;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReleaseItemSubItemsProcessor
        implements ItemProcessor<ReleaseItemSubItemsCommand, Collection<BaseEntity>> {

    private final EntityIdRegistry idRegistry;

    @Override
    public Collection<BaseEntity> process(ReleaseItemSubItemsCommand command) {
        if (command.getId() == null || command.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(command);
        List<BaseEntity> items = new ArrayList<>();
        long releaseItemId = command.getId();
        ReleaseItem releaseItem = getReleaseItem(releaseItemId);

        if (command.getMaster() != null) {
            Long masterId = command.getMaster().getMasterId();
            if (isExistingMaster(masterId)) {
                items.add(ReleaseItemMaster.builder()
                        .releaseItem(releaseItem)
                        .master(getMaster(masterId))
                        .build());
            }
        }

        if (command.getAlbumArtists() != null) {
            command.getAlbumArtists().stream()
                    .filter(Objects::nonNull)
                    .map(AlbumArtist::getId)
                    .distinct()
                    .filter(this::isExistingArtist)
                    .map(albumArtistId -> ReleaseItemArtist.builder()
                            .artist(getArtist(albumArtistId))
                            .releaseItem(releaseItem)
                            .build())
                    .distinct()
                    .forEach(items::add);
        }
        if (command.getCompanies() != null) {
            command.getCompanies().stream()
                    .filter(Objects::nonNull)
                    .filter(work -> isExistingLabel(work.getId()))
                    .map(work ->
                            ReleaseItemWork.builder()
                                    .label(getLabel(work.getId()))
                                    .releaseItem(releaseItem)
                                    .work(work.getWork())
                                    .build())
                    .distinct()
                    .forEach(items::add);
        }
        if (command.getCreditedArtists() != null) {
            command.getCreditedArtists().stream()
                    .filter(Objects::nonNull)
                    .filter(creditedArtist -> isExistingArtist(creditedArtist.getId()))
                    .map(creditedArtist ->
                            ReleaseItemCreditedArtist.builder()
                                    .role(creditedArtist.getRole())
                                    .artist(getArtist(creditedArtist.getId()))
                                    .releaseItem(releaseItem)
                                    .build())
                    .distinct()
                    .forEach(items::add);
        }
        if (command.getFormats() != null) {
            command.getFormats().stream()
                    .filter(Objects::nonNull)
                    .map(format -> ReleaseItemFormat.builder()
                            .releaseItem(releaseItem)
                            .quantity(format.getQty())
                            .text(format.getText().trim())
                            .name(format.getName().trim())
                            .description(getFormatDescription(format))
                            .build())
                    .distinct()
                    .forEach(items::add);
        }

        if (command.getGenres() != null) {
            command.getGenres().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(this::isExistingGenre)
                    .distinct()
                    .map(genre -> ReleaseItemGenre.builder().releaseItem(releaseItem).genre(getGenre(genre)).build())
                    .forEach(items::add);
        }

        if (command.getStyles() != null) {
            command.getStyles().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(this::isExistingStyle)
                    .distinct()
                    .map(style -> ReleaseItemStyle.builder()
                            .releaseItem(releaseItem)
                            .style(getStyle(style))
                            .build())
                    .forEach(items::add);
        }

        if (command.getIdentifiers() != null) {
            command.getIdentifiers().stream()
                    .filter(Objects::nonNull)
                    .map(identifier -> ReleaseItemIdentifier.builder()
                            .releaseItem(releaseItem)
                            .description(identifier.getDescription())
                            .type(identifier.getType())
                            .value(identifier.getValue())
                            .build())
                    .distinct()
                    .forEach(items::add);
        }

        if (command.getLabels() != null) {
            command.getLabels().stream()
                    .filter(Objects::nonNull)
                    .filter(label -> isExistingLabel(label.getId()))
                    .map(label -> LabelRelease.builder()
                            .categoryNotation(label.getCatno())
                            .label(getLabel(label.getId()))
                            .releaseItem(releaseItem)
                            .build())
                    .distinct()
                    .forEach(items::add);
        }

        if (command.getTracks() != null) {
            command.getTracks().stream()
                    .filter(Objects::nonNull)
                    .map(track -> ReleaseItemTrack.builder()
                            .duration(track.getDuration())
                            .position(track.getPosition())
                            .title(track.getTitle())
                            .releaseItem(releaseItem)
                            .build())
                    .distinct()
                    .forEach(items::add);
        }

        if (command.getVideos() != null) {
            command.getVideos().stream()
                    .filter(Objects::nonNull)
                    .filter(vid -> vid.getUrl() != null && !vid.getUrl().isBlank())
                    .map(video -> ReleaseItemVideo.builder()
                            .title(video.getTitle())
                            .description(video.getDescription())
                            .url(video.getUrl())
                            .releaseItem(releaseItem)
                            .build())
                    .distinct()
                    .forEach(items::add);
        }

        return items.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isExistingArtist(Long id) {
        if (id == null || id < 1) {
            return false;
        }
        return idRegistry.exists(EntityIdRegistry.Type.ARTIST, id);
    }

    private boolean isExistingLabel(Long id) {
        if (id == null || id < 1) {
            return false;
        }
        return idRegistry.exists(EntityIdRegistry.Type.LABEL, id);
    }

    private boolean isExistingMaster(Long id) {
        if (id == null || id < 1) {
            return false;
        }
        return idRegistry.exists(EntityIdRegistry.Type.MASTER, id);
    }

    private boolean isExistingGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return false;
        }
        return idRegistry.exists(EntityIdRegistry.Type.GENRE, genre);
    }

    private boolean isExistingStyle(String style) {
        if (style == null || style.isBlank()) {
            return false;
        }
        return idRegistry.exists(EntityIdRegistry.Type.STYLE, style);
    }

    private Artist getArtist(long artistId) {
        return Artist.builder().id(artistId).build();
    }

    private Master getMaster(long masterId) {
        return Master.builder().id(masterId).build();
    }

    private Label getLabel(long labelId) {
        return Label.builder().id(labelId).build();
    }

    private Genre getGenre(String genre) {
        return Genre.builder().name(genre).build();
    }

    private Style getStyle(String style) {
        return Style.builder().name(style).build();
    }

    private ReleaseItem getReleaseItem(long id) {
        return ReleaseItem.builder()
                .id(id)
                .build();
    }

    private String getFormatDescription(Format format) {
        if (format.getDescription() == null) {
            return null;
        }

        List<String> descriptions = format.getDescription().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(desc -> !desc.isBlank())
                .collect(Collectors.toList());

        if (descriptions.isEmpty()) {
            return null;
        }

        return descriptions.stream()
                .map(desc -> "[d:" + desc + "]")
                .collect(Collectors.joining(","));
    }
}
