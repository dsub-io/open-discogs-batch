package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyLiquibaseContractSourceUnitTest {

  private static final String RESOURCE_DIRECTORY =
      "io/dsub/opendiscogs/schema/contracts/";
  private static final String MANIFEST_NAME = "legacy-liquibase-v1.json";
  private static final String SCHEMA_NAME = "legacy-liquibase-v1.schema.json";
  private static final String VERIFIER_NAME = "legacy-schema-fingerprint-v1.sql";
  private static final List<String> RESOURCE_NAMES =
      List.of(MANIFEST_NAME, SCHEMA_NAME, VERIFIER_NAME);

  @TempDir Path temporaryDirectory;

  @Test
  void loadsTheExactPublishedModelContractIntoTypedRecords() {
    List<CanonicalMigration> migrations = new CanonicalMigrationSource().load();

    LegacyLiquibaseContract contract =
        new LegacyLiquibaseContractSource().load(migrations);

    assertThat(contract.migrations()).hasSize(7);
    assertThat(contract.schemaContracts()).extracting(LegacySchemaContract::prefix)
        .containsExactly("V004", "V006", "V007");
    assertThat(contract.schemaContractForLength(7).prefix()).isEqualTo("V007");
    assertThat(
            contract
                .migrations()
                .getFirst()
                .changeSetFor(LegacySchemaMode.PUBLIC)
                .policyFor(LegacyExecutionType.EXECUTED)
                .adoptionProof())
        .isEqualTo(LegacyAdoptionProof.EXACT_CHECKSUM);
  }

  @Test
  void rejectsUnknownContractLookupsInsteadOfGuessing() {
    LegacyLiquibaseContract contract =
        new LegacyLiquibaseContractSource().load(new CanonicalMigrationSource().load());

    assertThatThrownBy(() -> contract.schemaContractForPrefix("V005"))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("unsupported prefix V005");
    assertThatThrownBy(() -> contract.schemaContractForLength(5))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("row-count=5");
    assertThatThrownBy(
            () ->
                contract
                    .migrations()
                    .get(3)
                    .changeSetFor(LegacySchemaMode.PUBLIC)
                    .policyFor(LegacyExecutionType.MARK_RAN))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("does not permit MARK_RAN");
    LegacyMigrationContract incomplete =
        new LegacyMigrationContract("V001", "file", "checksum", List.of());
    assertThatThrownBy(() -> incomplete.changeSetFor(LegacySchemaMode.PUBLIC))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("has no PUBLIC changeset");
  }

  @Test
  void rejectsMissingAndDuplicateManifestResources() throws Exception {
    Path empty = temporaryDirectory.resolve("empty");
    Files.createDirectories(empty);
    try (URLClassLoader loader = loader(empty)) {
      assertThatThrownBy(
              () ->
                  new LegacyLiquibaseContractSource(loader)
                      .load(new CanonicalMigrationSource().load()))
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("missing classpath resource");
    }

    Path first = temporaryDirectory.resolve("first");
    Path second = temporaryDirectory.resolve("second");
    copyPublishedResources(first);
    copyPublishedResources(second);
    try (URLClassLoader loader =
        new URLClassLoader(new URL[] {first.toUri().toURL(), second.toUri().toURL()}, null)) {
      assertThatThrownBy(
              () ->
                  new LegacyLiquibaseContractSource(loader)
                      .load(new CanonicalMigrationSource().load()))
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("duplicate classpath resource");
    }
  }

  @Test
  void rejectsModifiedManifestSchemaAndVerifierBytes() throws Exception {
    Path manifestRoot = temporaryDirectory.resolve("manifest");
    copyPublishedResources(manifestRoot);
    Files.writeString(
        resource(manifestRoot, MANIFEST_NAME),
        publishedText(MANIFEST_NAME).replace("\"formatVersion\": 1", "\"formatVersion\": 2"));
    assertDefaultSourceFailure(manifestRoot, "legacy-liquibase-v1.json checksum mismatch");

    Path schemaRoot = temporaryDirectory.resolve("schema");
    copyPublishedResources(schemaRoot);
    Files.writeString(resource(schemaRoot, SCHEMA_NAME), publishedText(SCHEMA_NAME) + " ");
    assertDefaultSourceFailure(schemaRoot, "legacy-liquibase-v1.schema.json checksum mismatch");

    Path verifierRoot = temporaryDirectory.resolve("verifier");
    copyPublishedResources(verifierRoot);
    Files.writeString(resource(verifierRoot, VERIFIER_NAME), publishedText(VERIFIER_NAME) + " ");
    assertDefaultSourceFailure(verifierRoot, "legacy-schema-fingerprint-v1.sql checksum mismatch");
  }

  @Test
  void validatesTrustedManifestConstantsAndEnumsDefensively() throws Exception {
    assertManifestMutationFailure(
        "\"formatVersion\": 1", "\"formatVersion\": 2", "formatVersion mismatch");
    assertManifestMutationFailure(
        "open-discogs-legacy-liquibase/v1", "wrong-contract", "contract mismatch");
    assertManifestMutationFailure("\"liquibaseVersion\": \"5.0.3\"",
        "\"liquibaseVersion\": \"5.0.4\"", "liquibaseVersion mismatch");
    assertManifestMutationFailure(
        "\"liquibaseChecksumVersion\": 9",
        "\"liquibaseChecksumVersion\": 8",
        "liquibaseChecksumVersion mismatch");
    assertManifestMutationFailure(
        "legacy-liquibase-v1.schema.json", "unknown.schema.json", "name mismatch");
    assertManifestMutationFailure(
        "\"checksumPolicy\": \"EXACT\"",
        "\"checksumPolicy\": \"UNKNOWN\"",
        "unknown checksum policy");
    assertManifestMutationFailure(
        "\"schemaMode\": \"PUBLIC\"",
        "\"schemaMode\": \"UNKNOWN\"",
        "unknown LegacySchemaMode value");
  }

  @Test
  void rejectsAContractThatDoesNotMatchTheCanonicalArtifactPrefix() {
    List<CanonicalMigration> canonical = new CanonicalMigrationSource().load();

    assertThatThrownBy(
            () ->
                new LegacyLiquibaseContractSource()
                    .load(canonical.subList(0, 0)))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("newer than the canonical migration artifact");

    List<CanonicalMigration> wrongFilename = new java.util.ArrayList<>(canonical);
    CanonicalMigration first = wrongFilename.getFirst();
    wrongFilename.set(0, new CanonicalMigration("wrong.sql", first.checksum(), first.sql()));
    assertThatThrownBy(() -> new LegacyLiquibaseContractSource().load(wrongFilename))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("canonicalFilename mismatch");

    List<CanonicalMigration> wrongChecksum = new java.util.ArrayList<>(canonical);
    wrongChecksum.set(
        0, new CanonicalMigration(first.version(), "0".repeat(64), first.sql()));
    assertThatThrownBy(() -> new LegacyLiquibaseContractSource().load(wrongChecksum))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("canonicalSha256 mismatch");
  }

  @Test
  @SuppressWarnings("deprecation")
  void wrapsClasspathReadFailuresWithTheResourceIdentity() throws Exception {
    URL brokenResource =
        new URL(
            null,
            "memory:broken-contract",
            new URLStreamHandler() {
              @Override
              protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                  @Override
                  public void connect() {}

                  @Override
                  public InputStream getInputStream() throws IOException {
                    throw new IOException("fixture read failure");
                  }
                };
              }
            });
    ClassLoader loader =
        new ClassLoader(null) {
          @Override
          public Enumeration<URL> getResources(String name) {
            return Collections.enumeration(List.of(brokenResource));
          }
        };

    assertThatThrownBy(
            () ->
                new LegacyLiquibaseContractSource(loader)
                    .load(new CanonicalMigrationSource().load()))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("read classpath resource")
        .hasCauseInstanceOf(IOException.class);
  }

  private void assertManifestMutationFailure(
      String existing, String replacement, String expectedMessage) throws Exception {
    Path root = temporaryDirectory.resolve("mutation-" + Math.abs(existing.hashCode()));
    copyPublishedResources(root);
    String manifest = publishedText(MANIFEST_NAME).replaceFirst(existing, replacement);
    Files.writeString(resource(root, MANIFEST_NAME), manifest);
    try (URLClassLoader loader = loader(root)) {
      assertThatThrownBy(
              () ->
                  new LegacyLiquibaseContractSource(
                          loader, SchemaContractDigest.sha256(manifest))
                      .load(new CanonicalMigrationSource().load()))
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining(expectedMessage);
    }
  }

  private void assertDefaultSourceFailure(Path root, String expectedMessage) throws Exception {
    try (URLClassLoader loader = loader(root)) {
      assertThatThrownBy(
              () ->
                  new LegacyLiquibaseContractSource(loader)
                      .load(new CanonicalMigrationSource().load()))
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining(expectedMessage);
    }
  }

  private void copyPublishedResources(Path root) throws IOException {
    for (String name : RESOURCE_NAMES) {
      Path target = resource(root, name);
      Files.createDirectories(target.getParent());
      Files.writeString(target, publishedText(name));
    }
  }

  private String publishedText(String name) throws IOException {
    try (InputStream stream =
        LegacyLiquibaseContractSourceUnitTest.class
            .getClassLoader()
            .getResourceAsStream(RESOURCE_DIRECTORY + name)) {
      if (stream == null) {
        throw new IOException("missing published test resource " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Path resource(Path root, String name) {
    return root.resolve(RESOURCE_DIRECTORY).resolve(name);
  }

  private URLClassLoader loader(Path root) throws IOException {
    return new URLClassLoader(new URL[] {root.toUri().toURL()}, null);
  }
}
