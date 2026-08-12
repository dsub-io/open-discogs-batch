package io.dsub.discogs.batch.domain.release;

import io.dsub.discogs.batch.util.DiscogsStringNormalizer;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Computes the model-owned release relation identity v1 contract. */
public final class ReleaseRelationIdentity {

  private static final String IDENTITY_DOMAIN = "open-discogs/release-relation-identity/v1";
  private static final String SLOT_DOMAIN = "open-discogs/release-relation-slot/v1";
  private static final String SHA_256_ALGORITHM = "SHA-256";
  private static final byte DOMAIN_SEPARATOR = 0;
  private static final byte NULL_FIELD = 0;
  private static final byte PRESENT_FIELD = 1;

  private ReleaseRelationIdentity() {
  }

  public enum Relation {
    CREDITED_ARTIST("credited_artist"),
    FORMAT("format"),
    IDENTIFIER("identifier"),
    TRACK("track"),
    VIDEO("video"),
    WORK("work");

    private final String canonicalName;

    Relation(String canonicalName) {
      this.canonicalName = canonicalName;
    }
  }

  public static byte[] digest(Relation relation, String... fields) {
    ByteArrayOutputStream canonical = new ByteArrayOutputStream();
    writeString(canonical, IDENTITY_DOMAIN);
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
      throw new IllegalArgumentException("release relation digest must contain 32 bytes");
    }
    ByteArrayOutputStream canonical = new ByteArrayOutputStream();
    writeString(canonical, SLOT_DOMAIN);
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
