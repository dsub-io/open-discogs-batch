package io.dsub.discogs.batch.domain.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ReleaseRelationIdentityUnitTest {

  @Test
  void matchesTheCrossLanguageFormatFixture() {
    byte[] digest =
        ReleaseRelationIdentity.digest(
            ReleaseRelationIdentity.Relation.FORMAT,
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
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "AB", "C"))
        .isNotEqualTo(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "A", "BC"));
    assertThat(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, (String) null))
        .isEqualTo(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, ""));
    assertThat(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "Producer"))
        .isEqualTo(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "\u00a0Producer\u3000"));
    assertThat(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "Pro ducer"))
        .isNotEqualTo(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "Producer"));
  }

  @Test
  void matchesTheCrossLanguageCompatibilitySlotFixture() {
    byte[] digest =
        ReleaseRelationIdentity.digest(
            ReleaseRelationIdentity.Relation.TRACK, "6", "Яд");

    assertThat(
            ReleaseRelationIdentity.compatibilitySlot(
                ReleaseRelationIdentity.Relation.TRACK, digest, 0))
        .isEqualTo(1335459313);
  }

  @Test
  void rejectsInvalidDigestAndUnavailableAlgorithm() {
    assertThatThrownBy(
            () -> ReleaseRelationIdentity.compatibilitySlot(
                ReleaseRelationIdentity.Relation.TRACK, new byte[31], 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("32 bytes");
    assertThatThrownBy(() -> ReleaseRelationIdentity.digest(new byte[0], "unavailable"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unavailable is unavailable");
  }
}
