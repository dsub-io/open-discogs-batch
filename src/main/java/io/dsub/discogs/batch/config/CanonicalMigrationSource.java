package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Loads and validates canonical migrations packaged by open-discogs-model. */
final class CanonicalMigrationSource {

  private static final String RESOURCE_DIRECTORY =
      "io/dsub/opendiscogs/schema/migrations/";
  private static final String INDEX_RESOURCE = RESOURCE_DIRECTORY + "index.txt";
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("V(?<number>\\d{3})__[a-z0-9_]+\\.sql");

  private final ClassLoader resourceLoader;

  CanonicalMigrationSource() {
    this(CanonicalMigrationSource.class.getClassLoader());
  }

  CanonicalMigrationSource(ClassLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  List<CanonicalMigration> load() {
    try {
      String index = new String(readResource(INDEX_RESOURCE), StandardCharsets.UTF_8);
      List<String> versions = index.lines().filter(line -> !line.isBlank()).toList();
      validateInventory(versions);
      return versions.stream().map(this::loadMigration).toList();
    } catch (IOException exception) {
      throw new InitializationFailureException(
          "load canonical schema migrations: " + exception.getMessage(), exception);
    }
  }

  private void validateInventory(List<String> versions) throws IOException {
    if (versions.isEmpty()) {
      throw new IOException("canonical migration inventory is empty");
    }
    Set<String> uniqueVersions = new HashSet<>(versions);
    List<String> sortedVersions = versions.stream().sorted().toList();
    if (uniqueVersions.size() != versions.size() || !versions.equals(sortedVersions)) {
      throw new IOException("canonical migration inventory is duplicated or unsorted");
    }
    for (int index = 0; index < versions.size(); index++) {
      String version = versions.get(index);
      var matcher = VERSION_PATTERN.matcher(version);
      if (!matcher.matches() || Integer.parseInt(matcher.group("number")) != index + 1) {
        throw new IOException(
            "canonical migration inventory is invalid or non-contiguous at " + version);
      }
    }
  }

  private CanonicalMigration loadMigration(String version) {
    try {
      byte[] contents = readResource(RESOURCE_DIRECTORY + version);
      return new CanonicalMigration(
          version,
          SchemaContractDigest.sha256(contents),
          new String(contents, StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new InitializationFailureException(
          "load canonical schema migration " + version + ": " + exception.getMessage(),
          exception);
    }
  }

  private byte[] readResource(String name) throws IOException {
    try (InputStream stream = resourceLoader.getResourceAsStream(name)) {
      if (stream == null) {
        throw new IOException("missing classpath resource " + name);
      }
      return stream.readAllBytes();
    }
  }

}
