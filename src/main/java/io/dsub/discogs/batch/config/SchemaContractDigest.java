package io.dsub.discogs.batch.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Computes deterministic digests used by immutable schema contracts. */
final class SchemaContractDigest {

  private static final String SHA_256_ALGORITHM = "SHA-256";

  private SchemaContractDigest() {}

  static String sha256(byte[] value) {
    return digest(value, SHA_256_ALGORITHM);
  }

  static String sha256(String value) {
    return sha256(value.getBytes(StandardCharsets.UTF_8));
  }

  static String digest(byte[] value, String algorithm) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalArgumentException("unsupported digest algorithm " + algorithm, exception);
    }
  }
}
