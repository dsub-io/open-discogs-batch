package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand.Alias;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand.Group;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand.Member;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.artist.entity.*;
import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.ARTIST;

@RequiredArgsConstructor
public class ArtistSubItemsProcessor
        implements ItemProcessor<ArtistSubItemsCommand, Collection<BaseEntity>> {

    private final EntityIdRegistry idRegistry;

    @Override
    public Collection<BaseEntity> process(ArtistSubItemsCommand command) {

        if (command.getId() == null || command.getId() < 1) {
            return null;
        }

        ReflectionUtil.normalizeStringFields(command);

        List<BaseEntity> items = new LinkedList<>();

        long coreArtistId = command.getId();

        if (command.getAliases() != null) {
            command.getAliases().stream()
                    .filter(Objects::nonNull)
                    .map(Alias::getId)
                    .filter(this::isExistingArtist)
                    .distinct()
                    .map(aliasId -> getArtistAlias(coreArtistId, aliasId))
                    .forEach(items::add);
        }

        if (command.getGroups() != null) {
            command.getGroups().stream()
                    .filter(Objects::nonNull)
                    .map(Group::getId)
                    .filter(this::isExistingArtist)
                    .distinct()
                    .map(groupId -> getArtistGroup(coreArtistId, groupId))
                    .forEach(items::add);
        }

        if (command.getMembers() != null) {
            command.getMembers().stream()
                    .filter(Objects::nonNull)
                    .map(Member::getId)
                    .filter(this::isExistingArtist)
                    .map(memberId -> getArtistMember(coreArtistId, memberId))
                    .forEach(items::add);
        }

        if (command.getUrls() != null) {
            command.getUrls().stream()
                    .filter(Objects::nonNull)
                    .filter(url -> !url.isBlank())
                    .distinct()
                    .map(url -> getArtistUrl(coreArtistId, url))
                    .forEach(items::add);
        }

        if (command.getNameVariations() != null) {
            command.getNameVariations().stream()
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .map(nameVariation -> getArtistNameVariation(coreArtistId, nameVariation))
                    .forEach(items::add);
        }

        return items.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isExistingArtist(long artistId) {
        return idRegistry.exists(ARTIST, artistId);
    }

    private ArtistAlias getArtistAlias(Long coreArtistId, Long aliasId) {
        return ArtistAlias.builder()
                .artist(getArtist(coreArtistId))
                .alias(getArtist(aliasId))
                .build();
    }

    private ArtistGroup getArtistGroup(Long coreArtistId, Long groupId) {
        return ArtistGroup.builder()
                .artist(getArtist(coreArtistId))
                .group(getArtist(groupId))
                .build();
    }

    private ArtistMember getArtistMember(Long coreArtistId, Long memberId) {
        return ArtistMember.builder()
                .artist(getArtist(coreArtistId))
                .member(getArtist(memberId))
                .build();
    }

    private ArtistUrl getArtistUrl(Long coreArtistId, String url) {
        return ArtistUrl.builder()
                .artist(getArtist(coreArtistId))
                .url(url)
                .build();
    }

    private ArtistNameVariation getArtistNameVariation(Long coreArtistId, String nameVariation) {
        return ArtistNameVariation.builder()
                .artist(getArtist(coreArtistId))
                .name(nameVariation)
                .build();
    }

    private Artist getArtist(Long id) {
        return Artist.builder().id(id).build();
    }
}
