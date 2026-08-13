package io.dsub.discogs.batch.domain;

import io.dsub.discogs.batch.util.DiscogsStringNormalizer;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Computes the model-owned collision-resistant relation identity contracts. */
public final class CanonicalRelationIdentity {

  private static final String NON_RELEASE_IDENTITY_DOMAIN =
      "open-discogs/non-release-relation-identity/v1";
  private static final String NON_RELEASE_SLOT_DOMAIN =
      "open-discogs/non-release-relation-slot/v1";
  private static final String RELEASE_IDENTITY_DOMAIN =
      "open-discogs/release-relation-identity/v1";
  private static final String RELEASE_SLOT_DOMAIN = "open-discogs/release-relation-slot/v1";
  private static final String SHA_256_ALGORITHM = "SHA-256";
  private static final byte DOMAIN_SEPARATOR = 0;
  private static final byte NULL_FIELD = 0;
  private static final byte PRESENT_FIELD = 1;

  private CanonicalRelationIdentity() {
  }

  public enum Relation {
    ARTIST_NAME_VARIATION(
        "artist_name_variation", NON_RELEASE_IDENTITY_DOMAIN, NON_RELEASE_SLOT_DOMAIN),
    ARTIST_URL("artist_url", NON_RELEASE_IDENTITY_DOMAIN, NON_RELEASE_SLOT_DOMAIN),
    LABEL_URL("label_url", NON_RELEASE_IDENTITY_DOMAIN, NON_RELEASE_SLOT_DOMAIN),
    MASTER_VIDEO("master_video", NON_RELEASE_IDENTITY_DOMAIN, NON_RELEASE_SLOT_DOMAIN),
    CREDITED_ARTIST(
        "credited_artist", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    FORMAT("format", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    IDENTIFIER("identifier", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    IMAGE("image", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    TRACK("track", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    VIDEO("video", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN),
    WORK("work", RELEASE_IDENTITY_DOMAIN, RELEASE_SLOT_DOMAIN);

    private final String canonicalName;
    private final String identityDomain;
    private final String slotDomain;

    Relation(String canonicalName, String identityDomain, String slotDomain) {
      this.canonicalName = canonicalName;
      this.identityDomain = identityDomain;
      this.slotDomain = slotDomain;
    }
  }

  public static byte[] digest(Relation relation, String... fields) {
    ByteArrayOutputStream canonical = new ByteArrayOutputStream();
    writeString(canonical, relation.identityDomain);
    canonical.write(DOMAIN_SEPARATOR);
    writeString(canonical, relation.canonicalName);
    canonical.write(DOMAIN_SEPARATOR);
    for (String field : fields) {
      String normalized = DiscogsStringNormalizer.normalizeNullable(field);
      if (normalized == null) {
        canonical.write(NULL_FIELD);
        continue;
      }
      byte[] value = normalized.getBytes(StandardCharsets.UTF_8);
      canonical.write(PRESENT_FIELD);
      canonical.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
      canonical.writeBytes(value);
    }
    return sha256(canonical.toByteArray());
  }

  public static int compatibilitySlot(Relation relation, byte[] digest, int attempt) {
    if (digest.length != 32) {
      throw new IllegalArgumentException("relation digest must contain 32 bytes");
    }
    ByteArrayOutputStream canonical = new ByteArrayOutputStream();
    writeString(canonical, relation.slotDomain);
    canonical.write(DOMAIN_SEPARATOR);
    writeString(canonical, relation.canonicalName);
    canonical.write(DOMAIN_SEPARATOR);
    canonical.writeBytes(Arrays.copyOf(digest, digest.length));
    canonical.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(attempt).array());
    return ByteBuffer.wrap(sha256(canonical.toByteArray()), 0, Integer.BYTES).getInt();
  }

  private static byte[] sha256(byte[] value) {
    return digest(value, SHA_256_ALGORITHM);
  }

  static byte[] digest(byte[] value, String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm).digest(value);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(algorithm + " is unavailable", exception);
    }
  }

  private static void writeString(ByteArrayOutputStream output, String value) {
    output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
  }
}
