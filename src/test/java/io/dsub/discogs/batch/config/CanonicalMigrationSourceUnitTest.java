package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CanonicalMigrationSourceUnitTest {

  private static final String RESOURCE_DIRECTORY =
      "io/dsub/opendiscogs/schema/migrations/";
  private static final String INDEX_NAME = "index.txt";
  private static final String MIGRATION_NAME = "V001__fixture.sql";

  @TempDir Path temporaryDirectory;

  @Test
  void loadsAContiguousInventoryAndIgnoresBlankIndexLines() throws Exception {
    writeResource(INDEX_NAME, "\n" + MIGRATION_NAME + "\n\n");
    writeResource(MIGRATION_NAME, "select 1;\n");

    try (URLClassLoader loader = loader()) {
      List<CanonicalMigration> migrations = new CanonicalMigrationSource(loader).load();
      assertThat(migrations).hasSize(1);
      assertThat(migrations.getFirst().version()).isEqualTo(MIGRATION_NAME);
      assertThat(migrations.getFirst().checksum())
          .isEqualTo("4a45092ccf992ea92250053a80b931b787924ba61648f420555511b84f10ab6c");
      assertThat(migrations.getFirst().sql()).isEqualTo("select 1;\n");
    }
  }

  @Test
  void rejectsMissingAndEmptyInventories() throws Exception {
    try (URLClassLoader loader = loader()) {
      assertThatThrownBy(() -> new CanonicalMigrationSource(loader).load())
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("missing classpath resource");
    }

    writeResource(INDEX_NAME, "\n");
    try (URLClassLoader loader = loader()) {
      assertThatThrownBy(() -> new CanonicalMigrationSource(loader).load())
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("inventory is empty");
    }
  }

  @Test
  void rejectsDuplicateAndUnsortedInventories() throws Exception {
    assertInvalidIndex(MIGRATION_NAME + "\n" + MIGRATION_NAME, "duplicated or unsorted");
    assertInvalidIndex(
        "V002__second.sql\n" + MIGRATION_NAME, "duplicated or unsorted");
  }

  @Test
  void rejectsMalformedAndNonContiguousVersions() throws Exception {
    assertInvalidIndex("not-a-migration.sql", "invalid or non-contiguous");
    assertInvalidIndex("V002__second.sql", "invalid or non-contiguous");
  }

  @Test
  void rejectsAListedMigrationWhoseResourceIsMissing() throws Exception {
    writeResource(INDEX_NAME, MIGRATION_NAME);

    try (URLClassLoader loader = loader()) {
      assertThatThrownBy(() -> new CanonicalMigrationSource(loader).load())
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("load canonical schema migration")
          .hasMessageContaining("missing classpath resource");
    }
  }

  @Test
  void wrapsResourceReadFailures() {
    ClassLoader loader =
        new ClassLoader(null) {
          @Override
          public InputStream getResourceAsStream(String name) {
            return new InputStream() {
              @Override
              public int read() throws IOException {
                throw new IOException("fixture read failure");
              }
            };
          }
        };

    assertThatThrownBy(() -> new CanonicalMigrationSource(loader).load())
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("load canonical schema migrations")
        .hasCauseInstanceOf(IOException.class);
  }

  private void assertInvalidIndex(String index, String expectedMessage) throws Exception {
    writeResource(INDEX_NAME, index);
    try (URLClassLoader loader = loader()) {
      assertThatThrownBy(() -> new CanonicalMigrationSource(loader).load())
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining(expectedMessage);
    }
  }

  private void writeResource(String name, String contents) throws IOException {
    Path target = temporaryDirectory.resolve(RESOURCE_DIRECTORY).resolve(name);
    Files.createDirectories(target.getParent());
    Files.writeString(target, contents);
  }

  private URLClassLoader loader() throws IOException {
    return new URLClassLoader(new URL[] {temporaryDirectory.toUri().toURL()}, null);
  }
}
