package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SchemaContractDigestUnitTest {

  @Test
  void computesUtf8AndByteSha256Values() {
    String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    assertThat(SchemaContractDigest.sha256("abc")).isEqualTo(expected);
    assertThat(SchemaContractDigest.sha256("abc".getBytes(StandardCharsets.UTF_8)))
        .isEqualTo(expected);
  }

  @Test
  void rejectsUnsupportedDigestAlgorithmsWithTheCause() {
    assertThatThrownBy(() -> SchemaContractDigest.digest(new byte[0], "not-an-algorithm"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsupported digest algorithm")
        .hasCauseInstanceOf(java.security.NoSuchAlgorithmException.class);
  }
}
