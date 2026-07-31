package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.EntityType;

/**
 * Shared job-parameter keys used by import planning and execution.
 */
public final class ImportJobParameters {

  public static final String MANIFEST_SHA256 = "import.manifestSha256";
  public static final String FORCE = "import.force";
  public static final String ALLOW_DOWNGRADE = "import.allowDowngrade";

  private static final String DUMP_PREFIX = "import.dump.";

  private ImportJobParameters() {
  }

  public static String checksum(EntityType type) {
    return dumpKey(type, "checksumSha256");
  }

  public static String date(EntityType type) {
    return dumpKey(type, "date");
  }

  public static String etag(EntityType type) {
    return dumpKey(type, "etag");
  }

  public static String size(EntityType type) {
    return dumpKey(type, "sizeBytes");
  }

  public static String uri(EntityType type) {
    return dumpKey(type, "uri");
  }

  private static String dumpKey(EntityType type, String field) {
    return DUMP_PREFIX + type + "." + field;
  }
}
