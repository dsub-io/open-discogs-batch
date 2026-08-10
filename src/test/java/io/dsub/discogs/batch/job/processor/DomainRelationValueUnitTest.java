package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRelationValueUnitTest {

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

    assertThat(video.getRecord(7))
        .satisfies(
            record -> {
              assertThat(record.getTitle()).isEqualTo("Title");
              assertThat(record.getDescription()).isEqualTo("Description");
              assertThat(record.getUrl()).isEqualTo("https://video");
              assertThat(record.getHash()).isEqualTo(video.getHashValue());
            });
  }

  @Test
  void masterMainReleaseBuildHandlesMissingAndPresentMaster() {
    MasterMainReleaseXML value = new MasterMainReleaseXML();
    value.setReleaseId(11);
    assertThat(value.buildRecord().getId()).isNull();

    MasterMainReleaseXML.Master master = new MasterMainReleaseXML.Master();
    master.setMasterId(7);
    value.setMaster(master);
    assertThat(value.buildRecord())
        .satisfies(
            record -> {
              assertThat(record.getId()).isEqualTo(7);
              assertThat(record.getMainReleaseId()).isEqualTo(11);
            });
  }

  @Test
  void releaseFormatReducesDescriptionsAcrossAllBoundaryStates() {
    ReleaseItemSubItemsXML.ReleaseFormat format = new ReleaseItemSubItemsXML.ReleaseFormat();
    format.setName("Vinyl");
    assertThat(format.getRecord(1).getDescription()).isNull();

    format.setDescriptions(List.of());
    assertThat(format.getRecord(1).getDescription()).isNull();

    List<String> descriptions = new ArrayList<>();
    descriptions.add(null);
    descriptions.add("  ");
    format.setDescriptions(descriptions);
    assertThat(format.getRecord(1).getDescription()).isNull();

    format.setDescriptions(List.of(" LP ", "Limited"));
    assertThat(format.getRecord(1).getDescription()).isEqualTo("[d:LP],[d:Limited]");
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
