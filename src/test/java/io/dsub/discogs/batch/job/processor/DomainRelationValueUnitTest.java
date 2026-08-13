package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import jakarta.xml.bind.JAXBContext;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRelationValueUnitTest {

  private static final String OVERSIZED_FORMAT_QUANTITY =
      "1010487400000000000000000000000000000000000000000000";

  @Test
  void hashContractHandlesAbsentBlankAndPopulatedValues() {
    ReleaseItemSubItemsXML.ReleaseTrack value = new ReleaseItemSubItemsXML.ReleaseTrack();
    int objectHash = value.hashCode();

    assertThat(value.makeHash(null)).isEqualTo(objectHash);
    assertThat(value.makeHash(new String[0])).isEqualTo(objectHash);
    assertThat(value.makeHash(new String[] {null, "", "  "})).isEqualTo(objectHash);
    assertThat(value.makeHash(new String[] {" A ", null, "B"}))
        .isEqualTo(" A B".hashCode());
  }

  @Test
  void masterVideoBuildsACompleteParentBoundRecord() {
    MasterSubItemsXML.MasterVideoXML video = new MasterSubItemsXML.MasterVideoXML();
    video.setTitle("Title");
    video.setDescription("Description");
    video.setUrl("https://video");

    assertThat(video.getRecord(7, java.time.LocalDateTime.MIN))
        .satisfies(
            record -> {
              assertThat(record.getTitle()).isEqualTo("Title");
              assertThat(record.getDescription()).isEqualTo("Description");
              assertThat(record.getUrl()).isEqualTo("https://video");
              assertThat(record.getHash()).isEqualTo(video.getHashValue());
            });
  }

  @Test
  void releaseFormatReducesDescriptionsAcrossAllBoundaryStates() {
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName("Vinyl");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getDescription()).isNull();

    format.setDescriptions(List.of());
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getDescription()).isNull();

    List<String> descriptions = new ArrayList<>();
    descriptions.add(null);
    descriptions.add("  ");
    format.setDescriptions(descriptions);
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getDescription()).isNull();

    format.setDescriptions(List.of(" LP ", "Limited"));
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getDescription())
        .isEqualTo("[d:LP],[d:Limited]");
  }

  @Test
  void releaseFormatIdentityPreservesQuantityVariants() {
    ReleaseItemSubItemsXML.ReleaseFormat quantityOne = new ReleaseItemSubItemsXML.ReleaseFormat();
    quantityOne.setName("CD");
    quantityOne.setQuantity("1");
    quantityOne.setDescriptions(List.of("Compilation"));

    ReleaseItemSubItemsXML.ReleaseFormat quantityTwo = new ReleaseItemSubItemsXML.ReleaseFormat();
    quantityTwo.setName("CD");
    quantityTwo.setQuantity("2");
    quantityTwo.setDescriptions(List.of("Compilation"));

    assertThat(quantityOne.getHashValue()).isNotEqualTo(quantityTwo.getHashValue());
    assertThat(quantityOne.getRecord(48967, java.time.LocalDateTime.MIN).getHash())
        .isEqualTo(quantityOne.getHashValue());
    assertThat(quantityTwo.getRecord(48967, java.time.LocalDateTime.MIN).getHash())
        .isEqualTo(quantityTwo.getHashValue());
  }

  @Test
  void releaseFormatPreservesOversizedDiscogsQuantityAsCanonicalDecimal() {
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName("File");
    format.setQuantity(OVERSIZED_FORMAT_QUANTITY);

    var record = format.getRecord(6662697, java.time.LocalDateTime.MIN);
    assertThat(record.getQuantity()).isNull();
    assertThat(record.getQuantityText()).isEqualTo(OVERSIZED_FORMAT_QUANTITY);
    assertThat(record.getIdentitySha256()).hasSize(32);
  }

  @Test
  void releaseXmlParsesTheKnownOversizedQuantityWithoutIntegerCoercion() throws Exception {
    String xml =
        "<release id=\"6662697\"><formats><format name=\"File\" qty=\""
            + OVERSIZED_FORMAT_QUANTITY
            + "\"/></formats></release>";
    ReleaseItemSubItemsXML release =
        (ReleaseItemSubItemsXML)
            JAXBContext.newInstance(ReleaseItemSubItemsXML.class)
                .createUnmarshaller()
                .unmarshal(new StringReader(xml));

    assertThat(release.getReleaseFormats()).singleElement().satisfies(
        format -> {
          assertThat(format.getQuantity()).isEqualTo(OVERSIZED_FORMAT_QUANTITY);
          assertThat(format.getRecord(6_662_697, java.time.LocalDateTime.MIN).getQuantity()).isNull();
          assertThat(format.getRecord(6_662_697, java.time.LocalDateTime.MIN).getQuantityText())
              .isEqualTo(OVERSIZED_FORMAT_QUANTITY);
        });
  }

  @Test
  void releaseFormatCanonicalizesAndRejectsInvalidQuantities() {
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName("CD");
    format.setQuantity("0002");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantity()).isEqualTo(2);
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantityText()).isEqualTo("2");

    format.setQuantity(" \u3000");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantity()).isNull();
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantityText()).isNull();

    format.setQuantity("2147483647");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantity())
        .isEqualTo(Integer.MAX_VALUE);
    format.setQuantity("2147483648");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantity()).isNull();
    format.setQuantity("10000000000");
    assertThat(format.getRecord(1, java.time.LocalDateTime.MIN).getQuantity()).isNull();

    for (String invalid : List.of("-1", "not-a-number")) {
      format.setQuantity(invalid);
      assertThatThrownBy(() -> format.getRecord(1, java.time.LocalDateTime.MIN))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("invalid non-negative release format quantity");
    }
  }

  @Test
  void releaseRoleAndWorkHashesUseFallbackOnlyForMissingValues() {
    ReleaseItemSubItemsXML.ReleaseCreditedArtist artist =
        new ReleaseItemSubItemsXML.ReleaseCreditedArtist();
    assertThat(artist.getHashValue()).isEqualTo(artist.hashCode());
    artist.setRole(" ");
    assertThat(artist.getHashValue()).isEqualTo(artist.hashCode());
    artist.setRole("Producer");
    assertThat(artist.getHashValue()).isEqualTo("Producer".hashCode());

    ReleaseItemSubItemsXML.ReleaseWork work = new ReleaseItemSubItemsXML.ReleaseWork();
    assertThat(work.getHashValue()).isEqualTo(work.hashCode());
    work.setWork(" ");
    assertThat(work.getHashValue()).isEqualTo(work.hashCode());
    work.setWork("Pressed By");
    assertThat(work.getHashValue()).isEqualTo("Pressed By".hashCode());
  }
}
