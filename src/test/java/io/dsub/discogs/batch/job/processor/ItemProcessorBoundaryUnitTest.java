package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;

class ItemProcessorBoundaryUnitTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime OBSERVED_AT = LocalDateTime.of(2026, 8, 1, 0, 0);

  @Test
  void coreProcessorsRejectMissingIdsAndNormalizeValidRecords() throws Exception {
    ArtistCoreProcessor artistProcessor = new ArtistCoreProcessor();
    LabelCoreProcessor labelProcessor = new LabelCoreProcessor();
    MasterCoreProcessor masterProcessor = new MasterCoreProcessor();

    ArtistXML artist = new ArtistXML();
    assertThat(artistProcessor.process(artist, OBSERVED_AT)).isNull();
    artist.setId(0);
    assertThat(artistProcessor.process(artist, OBSERVED_AT)).isNull();
    artist.setId(1);
    artist.setName(" Artist ");
    assertThat(artistProcessor.process(artist, OBSERVED_AT).getName()).isEqualTo("Artist");

    LabelXML label = new LabelXML();
    assertThat(labelProcessor.process(label, OBSERVED_AT)).isNull();
    label.setId(-1);
    assertThat(labelProcessor.process(label, OBSERVED_AT)).isNull();
    label.setId(1);
    label.setName(" Label ");
    assertThat(labelProcessor.process(label, OBSERVED_AT).getName()).isEqualTo("Label");

    MasterXML master = new MasterXML();
    assertThat(masterProcessor.process(master, OBSERVED_AT)).isNull();
    master.setId(0);
    assertThat(masterProcessor.process(master, OBSERVED_AT)).isNull();
    master.setId(1);
    master.setTitle(" Master ");
    assertThat(masterProcessor.process(master, OBSERVED_AT).getTitle()).isEqualTo("Master");
  }

  @Test
  void coreSourceChunkSharesOneObservedTimestamp() throws Exception {
    ArtistXML first = new ArtistXML();
    first.setId(1);
    ArtistXML second = new ArtistXML();
    second.setId(2);
    SourceChunkItemProcessor<ArtistXML, io.dsub.opendiscogs.jooq.tables.records.ArtistRecord>
        processor = new SourceChunkItemProcessor<>(new ArtistCoreProcessor(), FIXED_CLOCK);

    var result =
        processor.process(
            new SourceChunk<>(new ChunkRange(0, 0, 2), List.of(first, second)));

    assertThat(result.values())
        .extracting(io.dsub.opendiscogs.jooq.tables.records.ArtistRecord::getCreatedAt)
        .containsOnly(LocalDateTime.of(2026, 8, 1, 0, 0));
    assertThat(result.values())
        .allSatisfy(
            record ->
                assertThat(record.getLastModifiedAt()).isEqualTo(record.getCreatedAt()));
  }

  @Test
  void releaseCoreProcessorHandlesEveryMasterReferenceState() throws Exception {
    EntityIdRegistry registry = registry();
    ReleaseItemCoreProcessor processor = new ReleaseItemCoreProcessor(registry);
    ReleaseItemXML release = new ReleaseItemXML();

    assertThat(processor.process(release, OBSERVED_AT)).isNull();
    release.setId(0);
    assertThat(processor.process(release, OBSERVED_AT)).isNull();

    release.setId(10);
    release.setTitle(" Release ");
    release.setReleaseDate("2026-07-01");
    assertThat(processor.process(release, OBSERVED_AT))
        .satisfies(
            record -> {
              assertThat(record.getTitle()).isEqualTo("Release");
              assertThat(record.getMasterId()).isNull();
              assertThat(record.getIsMaster()).isFalse();
            });

    ReleaseItemXML.Master master = new ReleaseItemXML.Master();
    master.setMaster(false);
    release.setMaster(master);
    assertThat(processor.process(release, OBSERVED_AT).getIsMaster()).isFalse();
    master.setMaster(true);
    assertThat(processor.process(release, OBSERVED_AT).getMasterId()).isNull();
    master.setMasterId(2);
    assertThat(processor.process(release, OBSERVED_AT).getMasterId()).isNull();
    master.setMasterId(1);
    assertThat(processor.process(release, OBSERVED_AT))
        .satisfies(
            record -> {
              assertThat(record.getMasterId()).isEqualTo(1);
              assertThat(record.getIsMaster()).isTrue();
            });
  }

  @Test
  void artistRelationsFilterInvalidValuesAndLeaveCanonicalDedupeToTheBatch() {
    ArtistSubItemsProcessor processor = new ArtistSubItemsProcessor(registry());
    ArtistSubItemsXML item = new ArtistSubItemsXML();

    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(0);
    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(1);
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();

    item.setAliases(List.of());
    item.setGroups(List.of());
    item.setMembers(List.of());
    item.setNameVariations(List.of());
    item.setUrls(List.of());
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();

    ArtistSubItemsXML.ArtistAliasXML alias = new ArtistSubItemsXML.ArtistAliasXML();
    alias.setAliasId(1);
    ArtistSubItemsXML.ArtistAliasXML missingAlias = new ArtistSubItemsXML.ArtistAliasXML();
    missingAlias.setAliasId(2);
    ArtistSubItemsXML.ArtistGroupXML group = new ArtistSubItemsXML.ArtistGroupXML();
    group.setGroupId(1);
    ArtistSubItemsXML.ArtistMemberXML member = new ArtistSubItemsXML.ArtistMemberXML();
    member.setMemberId(1);
    ArtistSubItemsXML.ArtistMemberXML missingMember = new ArtistSubItemsXML.ArtistMemberXML();
    missingMember.setMemberId(2);
    item.setAliases(listWithNull(alias, null, missingAlias, alias));
    item.setGroups(listWithNull(group, null, group));
    item.setMembers(listWithNull(member, null, missingMember, member));
    item.setNameVariations(listWithNull(" Name ", null, " ", "Name"));
    item.setUrls(listWithNull(" https://example.test ", null, "", "https://example.test"));

    RelationSet result = processor.process(item, OBSERVED_AT);
    assertThat(result.records()).hasSize(10);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ArtistAliasRecord.class))
        .containsExactly(0, 3);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ArtistGroupRecord.class))
        .containsExactly(0, 2);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ArtistMemberRecord.class))
        .containsExactly(0, 3);
    assertThat(
            ordinals(
                result,
                io.dsub.opendiscogs.jooq.tables.records.ArtistNameVariationRecord.class))
        .containsExactly(0, 3);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ArtistUrlRecord.class))
        .containsExactly(0, 3);
  }

  @Test
  void labelRelationsFilterInvalidValuesAndLeaveCanonicalDedupeToTheBatch() {
    LabelSubItemsProcessor processor = new LabelSubItemsProcessor(registry());
    LabelSubItemsXML item = new LabelSubItemsXML();

    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(-1);
    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(1);
    item.setLabelSubLabels(null);
    item.setUrls(null);
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();

    LabelSubItemsXML.LabelSubLabelXML existing = new LabelSubItemsXML.LabelSubLabelXML();
    existing.setSubLabelId(1);
    LabelSubItemsXML.LabelSubLabelXML missing = new LabelSubItemsXML.LabelSubLabelXML();
    missing.setSubLabelId(2);
    item.setLabelSubLabels(listWithNull(existing, null, missing, existing));
    item.setUrls(listWithNull(" https://example.test ", null, " ", "https://example.test"));

    RelationSet result = processor.process(item, OBSERVED_AT);
    assertThat(result.records()).hasSize(4);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.LabelSubLabelRecord.class))
        .containsExactly(0, 3);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.LabelUrlRecord.class))
        .containsExactly(0, 3);
  }

  @Test
  void masterRelationsCoverEmptyAndPopulatedReferenceCollections() {
    MasterSubItemsProcessor processor = new MasterSubItemsProcessor(registry());
    MasterSubItemsXML item = new MasterSubItemsXML();

    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(0);
    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(1);
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();
    item.setMasterArtists(List.of());
    item.setMasterVideos(List.of());
    item.setGenres(List.of());
    item.setStyles(List.of());
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();

    MasterSubItemsXML.MasterArtistXML artist = new MasterSubItemsXML.MasterArtistXML();
    artist.setArtistId(1);
    MasterSubItemsXML.MasterArtistXML missingArtist = new MasterSubItemsXML.MasterArtistXML();
    missingArtist.setArtistId(2);
    MasterSubItemsXML.MasterVideoXML completeVideo = video("Title", "Description", "https://one");
    MasterSubItemsXML.MasterVideoXML partialVideo = video(null, null, "https://two");
    MasterSubItemsXML.MasterVideoXML blankVideo = video(null, null, " ");
    item.setMasterArtists(listWithNull(artist, null, missingArtist, artist));
    item.setMasterVideos(listWithNull(completeVideo, null, partialVideo, blankVideo));
    item.setGenres(listWithNull(" Rock ", null, "Missing", "Rock"));
    item.setStyles(listWithNull(" House ", null, "Missing", "House"));

    RelationSet result = processor.process(item, OBSERVED_AT);
    assertThat(result.records()).hasSize(8);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.MasterArtistRecord.class))
        .containsExactly(0, 3);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.MasterVideoRecord.class))
        .containsExactly(0, 2);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.MasterGenreRecord.class))
        .containsExactly(0, 3);
    assertThat(ordinals(result, io.dsub.opendiscogs.jooq.tables.records.MasterStyleRecord.class))
        .containsExactly(0, 3);
  }

  @Test
  void releaseRelationsCoverEveryCollectionAndReferenceFilter() {
    ReleaseItemSubItemsProcessor processor = new ReleaseItemSubItemsProcessor(registry());
    ReleaseItemSubItemsXML item = new ReleaseItemSubItemsXML();

    assertThat(processor.process(null, OBSERVED_AT)).isNull();
    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(0);
    assertThat(processor.process(item, OBSERVED_AT)).isNull();
    item.setId(1);
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();
    setEmptyReleaseRelations(item);
    assertThat(processor.process(item, OBSERVED_AT).records()).isEmpty();

    ReleaseItemSubItemsXML.ReleaseAlbumArtist albumArtist =
        new ReleaseItemSubItemsXML.ReleaseAlbumArtist();
    albumArtist.setArtistId(1);
    ReleaseItemSubItemsXML.ReleaseAlbumArtist invalidAlbumArtist =
        new ReleaseItemSubItemsXML.ReleaseAlbumArtist();
    invalidAlbumArtist.setArtistId(0);
    ReleaseItemSubItemsXML.ReleaseAlbumArtist nullAlbumArtist =
        new ReleaseItemSubItemsXML.ReleaseAlbumArtist();
    ReleaseItemSubItemsXML.ReleaseCreditedArtist creditedArtist =
        new ReleaseItemSubItemsXML.ReleaseCreditedArtist();
    creditedArtist.setArtistId(1);
    creditedArtist.setRole("Producer");
    ReleaseItemSubItemsXML.ReleaseCreditedArtist missingCreditedArtist =
        new ReleaseItemSubItemsXML.ReleaseCreditedArtist();
    missingCreditedArtist.setArtistId(2);
    ReleaseItemSubItemsXML.ReleaseCreditedArtist blankCreditedArtist =
        new ReleaseItemSubItemsXML.ReleaseCreditedArtist();
    blankCreditedArtist.setArtistId(1);
    blankCreditedArtist.setRole(" ");
    ReleaseItemSubItemsXML.LabelItemRelease label =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    label.setLabelId(1);
    ReleaseItemSubItemsXML.LabelItemRelease invalidLabel =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    invalidLabel.setLabelId(null);
    ReleaseItemSubItemsXML.LabelItemRelease zeroLabel =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    zeroLabel.setLabelId(0);
    ReleaseItemSubItemsXML.ReleaseWork company = new ReleaseItemSubItemsXML.ReleaseWork();
    company.setId(1);
    company.setWork("Pressed By");
    ReleaseItemSubItemsXML.ReleaseWork missingCompany = new ReleaseItemSubItemsXML.ReleaseWork();
    missingCompany.setId(2);
    ReleaseItemSubItemsXML.ReleaseWork blankCompany = new ReleaseItemSubItemsXML.ReleaseWork();
    blankCompany.setId(1);
    blankCompany.setWork(" ");
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName("Vinyl");
    format.setDescriptions(listWithNull(" LP ", " "));
    ReleaseItemSubItemsXML.ReleaseFormat blankFormat = new ReleaseItemSubItemsXML.ReleaseFormat();
    ReleaseItemSubItemsXML.ReleaseIdentifier identifier =
        new ReleaseItemSubItemsXML.ReleaseIdentifier();
    identifier.setType("Barcode");
    identifier.setValue("1");
    ReleaseItemSubItemsXML.ReleaseIdentifier emptyIdentifier =
        new ReleaseItemSubItemsXML.ReleaseIdentifier();
    ReleaseItemSubItemsXML.ReleaseTrack track = new ReleaseItemSubItemsXML.ReleaseTrack();
    track.setPosition("A1");
    track.setTitle("Track");
    ReleaseItemSubItemsXML.ReleaseTrack emptyTrack = new ReleaseItemSubItemsXML.ReleaseTrack();
    ReleaseItemSubItemsXML.ReleaseVideo video = new ReleaseItemSubItemsXML.ReleaseVideo();
    video.setUrl("https://video");
    ReleaseItemSubItemsXML.ReleaseVideo invalidVideo = new ReleaseItemSubItemsXML.ReleaseVideo();
    invalidVideo.setUrl(null);
    item.setReleaseAlbumArtists(
        listWithNull(albumArtist, null, invalidAlbumArtist, nullAlbumArtist, albumArtist));
    item.setReleaseCreditedArtists(
        listWithNull(
            creditedArtist, null, missingCreditedArtist, blankCreditedArtist, creditedArtist));
    item.setLabelReleaseLabels(listWithNull(label, null, invalidLabel, zeroLabel, label));
    item.setCompanies(listWithNull(company, null, missingCompany, blankCompany, company));
    item.setReleaseFormats(listWithNull(format, null, blankFormat, format));
    item.setReleaseIdentifiers(listWithNull(identifier, null, emptyIdentifier, identifier));
    item.setReleaseTracks(listWithNull(track, null, emptyTrack, track));
    item.setReleaseVideos(listWithNull(video, null, invalidVideo, video));
    item.setGenres(listWithNull(" Rock ", null, " ", "Missing", "Rock"));
    item.setStyles(listWithNull(" House ", null, " ", "Missing", "House"));

    RelationSet result = processor.process(item, OBSERVED_AT);
    assertThat(result.records()).hasSize(22);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord.class))
        .containsExactly(0, 4);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemWorkRecord.class))
        .containsExactly(0, 4);
    assertThat(
            ordinals(
                result,
                io.dsub.opendiscogs.jooq.tables.records.ReleaseItemCreditedArtistRecord.class))
        .containsExactly(0, 4);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord.class))
        .containsExactly(0, 3);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord.class))
        .containsExactly(0, 3, 4);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord.class))
        .containsExactly(0, 3, 4);
    assertThat(
            ordinals(
                result,
                io.dsub.opendiscogs.jooq.tables.records.ReleaseItemIdentifierRecord.class))
        .containsExactly(0, 3);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord.class))
        .containsExactly(0, 4);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord.class))
        .containsExactly(0, 3);
    assertThat(
            ordinals(result, io.dsub.opendiscogs.jooq.tables.records.ReleaseItemVideoRecord.class))
        .containsExactly(0, 3);
  }

  @Test
  void releaseLabelsPreserveDistinctCatalogNumbers() {
    ReleaseItemSubItemsProcessor processor = new ReleaseItemSubItemsProcessor(registry());
    ReleaseItemSubItemsXML item = new ReleaseItemSubItemsXML();
    item.setId(2);
    ReleaseItemSubItemsXML.LabelItemRelease spaced =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    spaced.setLabelId(1);
    spaced.setCategoryNotation("SK 026");
    ReleaseItemSubItemsXML.LabelItemRelease compact =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    compact.setLabelId(1);
    compact.setCategoryNotation(" SK026 ");
    item.setLabelReleaseLabels(List.of(spaced, compact));

    assertThat(processor.process(item, OBSERVED_AT).records())
        .extracting(record -> record.get("category_notation"))
        .containsExactly("SK 026", "SK026");
  }

  private EntityIdRegistry registry() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    when(registry.exists(any(EntityIdRegistry.Type.class), nullable(Integer.class)))
        .thenAnswer(invocation -> Integer.valueOf(1).equals(invocation.getArgument(1)));
    when(registry.exists(any(EntityIdRegistry.Type.class), nullable(String.class)))
        .thenAnswer(
            invocation -> {
              String value = invocation.getArgument(1);
              return "Rock".equals(value) || "House".equals(value);
            });
    return registry;
  }

  private List<Integer> ordinals(
      RelationSet relationSet, Class<? extends UpdatableRecord<?>> recordType) {
    return relationSet.records().stream()
        .filter(recordType::isInstance)
        .map(record -> (Integer) record.get("ordinal"))
        .toList();
  }

  @SafeVarargs
  private <T> List<T> listWithNull(T... values) {
    return new java.util.ArrayList<>(java.util.Arrays.asList(values));
  }

  private MasterSubItemsXML.MasterVideoXML video(String title, String description, String url) {
    MasterSubItemsXML.MasterVideoXML video = new MasterSubItemsXML.MasterVideoXML();
    video.setTitle(title);
    video.setDescription(description);
    video.setUrl(url);
    return video;
  }

  private void setEmptyReleaseRelations(ReleaseItemSubItemsXML item) {
    item.setReleaseAlbumArtists(List.of());
    item.setCompanies(List.of());
    item.setReleaseCreditedArtists(List.of());
    item.setReleaseFormats(List.of());
    item.setGenres(List.of());
    item.setStyles(List.of());
    item.setReleaseIdentifiers(List.of());
    item.setLabelReleaseLabels(List.of());
    item.setReleaseTracks(List.of());
    item.setReleaseVideos(List.of());
  }
}
