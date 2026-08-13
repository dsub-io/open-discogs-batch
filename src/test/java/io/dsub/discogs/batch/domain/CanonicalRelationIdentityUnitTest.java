package io.dsub.discogs.batch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class CanonicalRelationIdentityUnitTest {

  @Test
  void matchesTheCrossLanguageFormatFixture() {
    byte[] digest =
        CanonicalRelationIdentity.digest(
            CanonicalRelationIdentity.Relation.FORMAT,
            "CD",
            null,
            "2",
            "Compilation");

    assertThat(HexFormat.of().formatHex(digest))
        .isEqualTo("7068967bd82bfe4cae04c757f20a9e8a28bbaf5fa7adc6a71137e3c377ec3d66");
  }

  @Test
  void framesNullAndCompositeFieldsUnambiguously() {
    assertThat(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "AB", "C"))
        .isNotEqualTo(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "A", "BC"));
    assertThat(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, (String) null))
        .isEqualTo(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, ""));
    assertThat(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "Producer"))
        .isEqualTo(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "\u00a0Producer\u3000"));
    assertThat(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "Pro ducer"))
        .isNotEqualTo(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.TRACK, "Producer"));
  }

  @Test
  void separatesTheKnownArtist33476NameVariationCollision() {
    String first = "Al Thompson";
    String second = "C. Thompson";

    assertThat(first.hashCode()).isEqualTo(-1_130_078_775).isEqualTo(second.hashCode());
    assertThat(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.ARTIST_NAME_VARIATION, first))
        .isNotEqualTo(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.ARTIST_NAME_VARIATION, second));
  }

  @Test
  void matchesTheCrossLanguageCompatibilitySlotFixture() {
    byte[] digest =
        CanonicalRelationIdentity.digest(
            CanonicalRelationIdentity.Relation.TRACK, "6", "Яд");

    assertThat(
            CanonicalRelationIdentity.compatibilitySlot(
                CanonicalRelationIdentity.Relation.TRACK, digest, 0))
        .isEqualTo(1335459313);
  }

  @Test
  void rejectsInvalidDigestAndUnavailableAlgorithm() {
    assertThatThrownBy(
            () -> CanonicalRelationIdentity.compatibilitySlot(
                CanonicalRelationIdentity.Relation.TRACK, new byte[31], 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("32 bytes");
    assertThatThrownBy(() -> CanonicalRelationIdentity.digest(new byte[0], "unavailable"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unavailable is unavailable");
  }
}
