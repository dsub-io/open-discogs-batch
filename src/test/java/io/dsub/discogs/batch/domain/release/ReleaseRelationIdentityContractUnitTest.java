package io.dsub.discogs.batch.domain.release;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML.ReleaseFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReleaseRelationIdentityContractUnitTest {

  private static final String VECTOR_RESOURCE =
      "/contracts/release-relation-identity-v1.tsv";
  private static final String VECTOR_NULL = "null";
  private static final String VECTOR_UNUSED = "-";
  private static final String VECTOR_HEX_PREFIX = "hex:";
  private static final int VECTOR_COLUMN_COUNT = 11;
  private static final List<String> VECTOR_HEADER =
      List.of(
          "kind",
          "id",
          "relation",
          "field_1",
          "field_2",
          "field_3",
          "field_4",
          "identity_sha256",
          "attempt",
          "slot",
          "expected");
  private static final Map<String, Integer> FIELD_COUNTS =
      Map.of(
          "credited_artist", 1,
          "format", 4,
          "identifier", 3,
          "image", 1,
          "track", 3,
          "video", 3,
          "work", 1);

  @Test
  void matchesCanonicalModelVectors() throws IOException {
    List<Vector> vectors = readVectors();
    Set<String> digestRelations = new HashSet<>();
    for (Vector vector : vectors) {
      switch (vector.kind()) {
        case "digest" -> {
          digestRelations.add(vector.relation());
          assertDigest(vector);
        }
        case "slot" -> assertSlot(vector);
        case "description" -> assertDescription(vector);
        default -> throw new IllegalArgumentException(
            "unknown vector kind " + vector.kind() + " for " + vector.id());
      }
    }
    assertThat(digestRelations).containsExactlyInAnyOrderElementsOf(FIELD_COUNTS.keySet());
  }

  private static List<Vector> readVectors() throws IOException {
    InputStream input =
        ReleaseRelationIdentityContractUnitTest.class.getResourceAsStream(VECTOR_RESOURCE);
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
                List.of(columns[3], columns[4], columns[5], columns[6]),
                columns[7],
                columns[8],
                columns[9],
                columns[10]));
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
        ReleaseRelationIdentity.digest(relation(vector.relation()), fields.toArray(String[]::new));
    assertThat(HexFormat.of().formatHex(actual))
        .as(vector.id())
        .isEqualTo(vector.identitySha256());
  }

  private static void assertSlot(Vector vector) {
    byte[] digest = HexFormat.of().parseHex(vector.identitySha256());
    int actual =
        ReleaseRelationIdentity.compatibilitySlot(
            relation(vector.relation()), digest, Integer.parseUnsignedInt(vector.attempt()));
    assertThat(actual).as(vector.id()).isEqualTo(Integer.parseInt(vector.slot()));
  }

  private static void assertDescription(Vector vector) {
    List<String> values = vector.fields().stream()
        .map(value -> decodeValue(vector.id(), value))
        .toList();
    ReleaseFormat format = new ReleaseFormat();
    format.setDescriptions(values);
    String actual = format.getRecord(1, LocalDateTime.MIN).getDescription();
    assertThat(actual).as(vector.id()).isEqualTo(decodeValue(vector.id(), vector.expected()));
  }

  private static ReleaseRelationIdentity.Relation relation(String canonicalName) {
    return ReleaseRelationIdentity.Relation.valueOf(canonicalName.toUpperCase(Locale.ROOT));
  }

  private static String decodeValue(String id, String encoded) {
    if (VECTOR_NULL.equals(encoded)) {
      return null;
    }
    if (!encoded.startsWith(VECTOR_HEX_PREFIX)) {
      throw new IllegalArgumentException(
          "vector " + id + " value is not null or hex: " + encoded);
    }
    byte[] value = HexFormat.of().parseHex(encoded.substring(VECTOR_HEX_PREFIX.length()));
    return new String(value, StandardCharsets.UTF_8);
  }

  private record Vector(
      String kind,
      String id,
      String relation,
      List<String> fields,
      String identitySha256,
      String attempt,
      String slot,
      String expected) {
  }
}
