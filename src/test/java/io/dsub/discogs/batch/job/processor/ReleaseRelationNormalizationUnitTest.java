package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseRelationNormalizationUnitTest {

  private static final int RELEASE_ID = 2;

  @Test
  void normalizesBeforeDomainDeduplicationAndPreservesCatalogSpelling() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    when(registry.exists(any(EntityIdRegistry.Type.class), any(Integer.class))).thenReturn(true);
    when(registry.exists(any(EntityIdRegistry.Type.class), any(String.class))).thenReturn(true);
    ReleaseItemSubItemsXML item = new ReleaseItemSubItemsXML();
    item.setId(RELEASE_ID);
    item.setGenres(List.of(" Rock ", "Rock"));
    item.setStyles(List.of(" House ", "House"));
    item.setReleaseFormats(List.of(format(" Vinyl ", " LP "), format("Vinyl", "LP")));
    item.setLabelReleaseLabels(
        List.of(label(null), label(null), label("SK 026"), label(" SK026 ")));

    RelationSet result = new ReleaseItemSubItemsProcessor(registry).process(item);

    assertThat(result.records()).filteredOn(ReleaseItemGenreRecord.class::isInstance).hasSize(1);
    assertThat(result.records()).filteredOn(ReleaseItemStyleRecord.class::isInstance).hasSize(1);
    assertThat(result.records()).filteredOn(ReleaseItemFormatRecord.class::isInstance).hasSize(1);
    assertThat(result.records())
        .filteredOn(LabelReleaseItemRecord.class::isInstance)
        .extracting(record -> record.get("category_notation"))
        .containsExactly(null, "SK 026", "SK026");
  }

  @Test
  void formatQuantityChangesTheNormalizedIdentityHash() {
    ReleaseItemSubItemsXML.ReleaseFormat first = format(" Vinyl ", " LP ");
    first.setQuantity("1");
    ReleaseItemSubItemsXML.ReleaseFormat second = format("Vinyl", "LP");
    second.setQuantity("2");
    ReflectionUtil.normalizeReleaseStringFields(first);
    ReflectionUtil.normalizeReleaseStringFields(second);

    assertThat(first.getRecord(RELEASE_ID).getHash())
        .isNotEqualTo(second.getRecord(RELEASE_ID).getHash());
    assertThat(first.getRecord(RELEASE_ID).getQuantity()).isEqualTo(1);
    assertThat(second.getRecord(RELEASE_ID).getQuantity()).isEqualTo(2);
  }

  @Test
  void keepsDescriptionOnlyFormatsAndDropsCompletelyBlankFormats() throws Exception {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    ReleaseItemSubItemsXML item = new ReleaseItemSubItemsXML();
    item.setId(RELEASE_ID);
    ReleaseItemSubItemsXML.ReleaseFormat described = new ReleaseItemSubItemsXML.ReleaseFormat();
    described.setDescriptions(Arrays.asList(null, " ", " Vinyl "));
    ReleaseItemSubItemsXML.ReleaseFormat blank = new ReleaseItemSubItemsXML.ReleaseFormat();
    blank.setDescriptions(Arrays.asList(null, " "));
    ReleaseItemSubItemsXML.ReleaseFormat missingDescriptions =
        new ReleaseItemSubItemsXML.ReleaseFormat();
    item.setReleaseFormats(List.of(described, blank, missingDescriptions));

    RelationSet result = new ReleaseItemSubItemsProcessor(registry).process(item);

    assertThat(result.records()).filteredOn(ReleaseItemFormatRecord.class::isInstance).hasSize(1);
  }

  private ReleaseItemSubItemsXML.ReleaseFormat format(String name, String description) {
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName(name);
    format.setDescriptions(List.of(description));
    format.setText("Limited");
    return format;
  }

  private ReleaseItemSubItemsXML.LabelItemRelease label(String categoryNotation) {
    ReleaseItemSubItemsXML.LabelItemRelease label =
        new ReleaseItemSubItemsXML.LabelItemRelease();
    label.setLabelId(5);
    label.setCategoryNotation(categoryNotation);
    return label;
  }
}
