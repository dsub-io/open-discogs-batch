package io.dsub.discogs.batch.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NonReleaseRelationIdentityContractUnitTest {

  private static final String VECTOR_RESOURCE =
      "/contracts/non-release-relation-identity-v1.tsv";
  private static final String VECTOR_NULL = "null";
  private static final String VECTOR_UNUSED = "-";
  private static final String VECTOR_HEX_PREFIX = "hex:";
  private static final int VECTOR_COLUMN_COUNT = 10;
  private static final List<String> VECTOR_HEADER =
      List.of(
          "kind",
          "id",
          "relation",
          "field_1",
          "field_2",
          "field_3",
          "identity_sha256",
          "attempt",
          "slot",
          "legacy_java_hash");
  private static final Map<String, Integer> FIELD_COUNTS =
      Map.of(
          "artist_name_variation", 1,
          "artist_url", 1,
          "label_url", 1,
          "master_video", 3);

  @Test
  void matchesCanonicalModelVectors() throws IOException {
    for (Vector vector : readVectors()) {
      switch (vector.kind()) {
        case "digest" -> assertDigest(vector);
        case "slot" -> assertSlot(vector);
        default ->
            throw new IllegalArgumentException(
                "unknown vector kind " + vector.kind() + " for " + vector.id());
      }
    }
  }

  private static List<Vector> readVectors() throws IOException {
    InputStream input =
        NonReleaseRelationIdentityContractUnitTest.class.getResourceAsStream(VECTOR_RESOURCE);
    assertThat(input).as(VECTOR_RESOURCE).isNotNull();
    try (input;
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      assertThat(Arrays.asList(reader.readLine().split("\\t", -1))).isEqualTo(VECTOR_HEADER);
      List<Vector> vectors = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] columns = line.split("\\t", -1);
        if (columns.length != VECTOR_COLUMN_COUNT) {
          throw new IllegalArgumentException(
              "vector column count = " + columns.length + ", want " + VECTOR_COLUMN_COUNT);
        }
        vectors.add(
            new Vector(
                columns[0],
                columns[1],
                columns[2],
                List.of(columns[3], columns[4], columns[5]),
                columns[6],
                columns[7],
                columns[8],
                columns[9]));
      }
      return List.copyOf(vectors);
    }
  }

  private static void assertDigest(Vector vector) {
    int fieldCount = FIELD_COUNTS.get(vector.relation());
    List<String> fields = new ArrayList<>(fieldCount);
    for (int index = 0; index < vector.fields().size(); index++) {
      String encoded = vector.fields().get(index);
      if (index >= fieldCount) {
        assertThat(encoded).as(vector.id()).isEqualTo(VECTOR_UNUSED);
      } else {
        fields.add(decodeValue(vector.id(), encoded));
      }
    }
    byte[] actual =
        CanonicalRelationIdentity.digest(
            relation(vector.relation()), fields.toArray(String[]::new));
    assertThat(HexFormat.of().formatHex(actual))
        .as(vector.id())
        .isEqualTo(vector.identitySha256());
    if (!VECTOR_UNUSED.equals(vector.legacyJavaHash())) {
      assertThat(fields.getFirst().hashCode())
          .as(vector.id() + " legacy Java hash")
          .isEqualTo(Integer.parseInt(vector.legacyJavaHash()));
    }
  }

  private static void assertSlot(Vector vector) {
    int actual =
        CanonicalRelationIdentity.compatibilitySlot(
            relation(vector.relation()),
            HexFormat.of().parseHex(vector.identitySha256()),
            Integer.parseUnsignedInt(vector.attempt()));
    assertThat(actual).as(vector.id()).isEqualTo(Integer.parseInt(vector.slot()));
  }

  private static CanonicalRelationIdentity.Relation relation(String canonicalName) {
    return CanonicalRelationIdentity.Relation.valueOf(canonicalName.toUpperCase(Locale.ROOT));
  }

  private static String decodeValue(String id, String encoded) {
    if (VECTOR_NULL.equals(encoded)) {
      return null;
    }
    if (!encoded.startsWith(VECTOR_HEX_PREFIX)) {
      throw new IllegalArgumentException(
          "vector " + id + " value is not null or hex: " + encoded);
    }
    return new String(
        HexFormat.of().parseHex(encoded.substring(VECTOR_HEX_PREFIX.length())),
        StandardCharsets.UTF_8);
  }

  private record Vector(
      String kind,
      String id,
      String relation,
      List<String> fields,
      String identitySha256,
      String attempt,
      String slot,
      String legacyJavaHash) {
  }
}
