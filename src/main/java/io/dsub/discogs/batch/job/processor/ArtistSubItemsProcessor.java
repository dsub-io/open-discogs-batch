package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.jooq.postgres.tables.records.*;
import lombok.RequiredArgsConstructor;
import org.jooq.UpdatableRecord;
import org.springframework.batch.item.ItemProcessor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.ARTIST;

@RequiredArgsConstructor
public class ArtistSubItemsProcessor implements ItemProcessor<ArtistSubItemsXML, Collection<UpdatableRecord<?>>> {

    private final EntityIdRegistry idRegistry;

    @Override
    public Collection<UpdatableRecord<?>> process(ArtistSubItemsXML item) {

        if (item.getId() == null || item.getId() < 1) {
            return null;
        }

        ReflectionUtil.normalizeStringFields(item);

        List<UpdatableRecord<?>> items = new ArrayList<>();

        items.addAll(getArtistAliasRecords(item));
        items.addAll(getArtistGroupRecords(item));
        items.addAll(getArtistMemberRecords(item));
        items.addAll(getArtistUrlRecords(item));
        items.addAll(getArtistNameVariationRecords(item));

        return items;
    }

    private List<ArtistNameVariationRecord> getArtistNameVariationRecords(ArtistSubItemsXML item) {
        if (item.getNameVariations() == null || item.getNameVariations().isEmpty()) {
            return Collections.emptyList();
        }
        return item.getNameVariations().stream()
                .filter(Objects::nonNull)
                .filter(nameVar -> !nameVar.isBlank())
                .distinct()
                .map(nameVar -> makeArtistNameVariationRecord(item.getId(), nameVar))
                .collect(Collectors.toList());
    }

    private List<ArtistUrlRecord> getArtistUrlRecords(ArtistSubItemsXML item) {
        if (item.getUrls() == null || item.getUrls().isEmpty()) {
            return Collections.emptyList();
        }
        return item.getUrls().stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .distinct()
                .map(url -> makeArtistUrlRecord(item.getId(), url))
                .collect(Collectors.toList());
    }

    private List<ArtistMemberRecord> getArtistMemberRecords(ArtistSubItemsXML item) {
        if (item.getMembers() == null || item.getMembers().isEmpty()) {
            return Collections.emptyList();
        }
        return item.getMembers().stream()
                .filter(member -> idRegistry.exists(ARTIST, member.getMemberId()))
                .map(xml -> xml.getRecord(item.getId()))
                .collect(Collectors.toList());
    }

    private List<ArtistGroupRecord> getArtistGroupRecords(ArtistSubItemsXML item) {
        if (item.getGroups() == null || item.getGroups().isEmpty()) {
            return Collections.emptyList();
        }
        return item.getGroups().stream()
                .filter(group -> idRegistry.exists(ARTIST, group.getGroupId()))
                .map(xml -> xml.getRecord(item.getId()))
                .collect(Collectors.toList());
    }

    private List<ArtistAliasRecord> getArtistAliasRecords(ArtistSubItemsXML item) {
        if (item.getAliases() == null || item.getAliases().isEmpty()) {
            return Collections.emptyList();
        }
        return item.getAliases().stream()
                .filter(alias -> idRegistry.exists(ARTIST, alias.getAliasId()))
                .map(xml -> xml.getRecord(item.getId()))
                .collect(Collectors.toList());
    }

    private ArtistNameVariationRecord makeArtistNameVariationRecord(Integer artistId, String nameVar) {
        ArtistNameVariationRecord record = new ArtistNameVariationRecord();
        return record.setArtistId(artistId)
                .setNameVariation(nameVar)
                .setHash(nameVar.hashCode())
                .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()))
                .setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
    }

    private ArtistUrlRecord makeArtistUrlRecord(Integer artistId, String url) {
        ArtistUrlRecord record = new ArtistUrlRecord();
        return record.setUrl(url)
                .setArtistId(artistId)
                .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
                .setHash(url.hashCode())
                .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
}
