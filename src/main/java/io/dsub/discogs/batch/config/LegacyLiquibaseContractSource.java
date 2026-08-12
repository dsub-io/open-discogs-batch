package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/** Loads the exact model 0.3.0 legacy Liquibase adoption contract. */
final class LegacyLiquibaseContractSource {

  private static final String RESOURCE_DIRECTORY = "io/dsub/opendiscogs/schema/contracts/";
  private static final String MANIFEST_NAME = "legacy-liquibase-v1.json";
  private static final String SCHEMA_DEFINITION_NAME = "legacy-liquibase-v1.schema.json";
  private static final String VERIFIER_NAME = "legacy-schema-fingerprint-v1.sql";
  private static final String MANIFEST_RESOURCE = RESOURCE_DIRECTORY + MANIFEST_NAME;
  private static final String EXPECTED_MANIFEST_SHA_256 =
      "0163ff3b5f901f1925e2e7afd1af412ebc5bc33c156e6c82b28bef5e5b66dd07";
  private static final String EXPECTED_SCHEMA_DEFINITION_SHA_256 =
      "33df80a1c3832dc71d8c5c43eabbd17f90ec12f949a8e83dc130fe7e342b5bf6";
  private static final String EXPECTED_VERIFIER_SHA_256 =
      "a33713235ad9832b378b25761b7bbc3efdd1dd4436c9ad9d87afb18e40960c65";
  private static final String CONTRACT_NAME = "open-discogs-legacy-liquibase/v1";
  private static final String LIQUIBASE_VERSION = "5.0.3";
  private static final int FORMAT_VERSION = 1;
  private static final int LIQUIBASE_CHECKSUM_VERSION = 9;

  private static final String FIELD_FORMAT_VERSION = "formatVersion";
  private static final String FIELD_CONTRACT = "contract";
  private static final String FIELD_SCHEMA_DEFINITION = "schemaDefinition";
  private static final String FIELD_LIQUIBASE_VERSION = "liquibaseVersion";
  private static final String FIELD_LIQUIBASE_CHECKSUM_VERSION = "liquibaseChecksumVersion";
  private static final String FIELD_SCHEMA_CONTRACTS = "schemaContracts";
  private static final String FIELD_MIGRATIONS = "migrations";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_SHA_256 = "sha256";
  private static final String FIELD_PREFIX = "prefix";
  private static final String FIELD_MIGRATION_VERSIONS = "migrationVersions";
  private static final String FIELD_VERIFIER = "verifier";
  private static final String FIELD_EXPECTED_FINGERPRINTS = "expectedFingerprints";
  private static final String FIELD_POSTGRES_MAJOR = "postgresMajor";
  private static final String FIELD_VERSION = "version";
  private static final String FIELD_CANONICAL_FILENAME = "canonicalFilename";
  private static final String FIELD_CANONICAL_SHA_256 = "canonicalSha256";
  private static final String FIELD_LEGACY_CHANGESETS = "legacyChangeSets";
  private static final String FIELD_SCHEMA_MODE = "schemaMode";
  private static final String FIELD_ID = "id";
  private static final String FIELD_AUTHOR = "author";
  private static final String FIELD_FILENAME = "filename";
  private static final String FIELD_CHECKSUM_POLICY = "checksumPolicy";
  private static final String FIELD_CHECKSUM = "checksum";
  private static final String FIELD_SCHEMA_PARAMETER = "schemaParameter";
  private static final String FIELD_EXECUTION_POLICIES = "executionPolicies";
  private static final String FIELD_EXECUTION_TYPE = "executionType";
  private static final String FIELD_ADOPTION_PROOF = "adoptionProof";
  private static final String EXACT_CHECKSUM_POLICY = "EXACT";
  private static final String SCHEMA_PARAMETERIZED_CHECKSUM_POLICY = "SCHEMA_PARAMETERIZED";

  private static final Set<String> ROOT_FIELDS =
      Set.of(
          FIELD_FORMAT_VERSION,
          FIELD_CONTRACT,
          FIELD_SCHEMA_DEFINITION,
          FIELD_LIQUIBASE_VERSION,
          FIELD_LIQUIBASE_CHECKSUM_VERSION,
          FIELD_SCHEMA_CONTRACTS,
          FIELD_MIGRATIONS);
  private static final Set<String> RESOURCE_FIELDS = Set.of(FIELD_NAME, FIELD_SHA_256);
  private static final Set<String> SCHEMA_CONTRACT_FIELDS =
      Set.of(
          FIELD_PREFIX,
          FIELD_MIGRATION_VERSIONS,
          FIELD_VERIFIER,
          FIELD_EXPECTED_FINGERPRINTS);
  private static final Set<String> FINGERPRINT_FIELDS =
      Set.of(FIELD_POSTGRES_MAJOR, FIELD_SHA_256);
  private static final Set<String> MIGRATION_FIELDS =
      Set.of(
          FIELD_VERSION,
          FIELD_CANONICAL_FILENAME,
          FIELD_CANONICAL_SHA_256,
          FIELD_LEGACY_CHANGESETS);
  private static final Set<String> CHANGESET_REQUIRED_FIELDS =
      Set.of(
          FIELD_SCHEMA_MODE,
          FIELD_ID,
          FIELD_AUTHOR,
          FIELD_FILENAME,
          FIELD_CHECKSUM_POLICY,
          FIELD_EXECUTION_POLICIES);
  private static final Set<String> CHANGESET_OPTIONAL_FIELDS =
      Set.of(FIELD_CHECKSUM, FIELD_SCHEMA_PARAMETER);
  private static final Set<String> EXECUTION_POLICY_FIELDS =
      Set.of(FIELD_EXECUTION_TYPE, FIELD_ADOPTION_PROOF);

  private final ClassLoader resourceLoader;
  private final String expectedManifestSha256;

  LegacyLiquibaseContractSource() {
    this(
        LegacyLiquibaseContractSource.class.getClassLoader(),
        EXPECTED_MANIFEST_SHA_256);
  }

  LegacyLiquibaseContractSource(ClassLoader resourceLoader) {
    this(resourceLoader, EXPECTED_MANIFEST_SHA_256);
  }

  LegacyLiquibaseContractSource(ClassLoader resourceLoader, String expectedManifestSha256) {
    this.resourceLoader = resourceLoader;
    this.expectedManifestSha256 = expectedManifestSha256;
  }

  LegacyLiquibaseContract load(List<CanonicalMigration> canonicalMigrations) {
    byte[] manifest = readUniqueResource(MANIFEST_RESOURCE);
    requireChecksum(MANIFEST_NAME, expectedManifestSha256, manifest);
    StrictJsonObject root =
        StrictJsonObject.parse(
            new String(manifest, StandardCharsets.UTF_8), MANIFEST_RESOURCE);
    root.requireFields(ROOT_FIELDS, Set.of());
    requireEqual(FORMAT_VERSION, root.integer(FIELD_FORMAT_VERSION), FIELD_FORMAT_VERSION);
    requireEqual(CONTRACT_NAME, root.string(FIELD_CONTRACT), FIELD_CONTRACT);
    requireEqual(
        LIQUIBASE_VERSION, root.string(FIELD_LIQUIBASE_VERSION), FIELD_LIQUIBASE_VERSION);
    requireEqual(
        LIQUIBASE_CHECKSUM_VERSION,
        root.integer(FIELD_LIQUIBASE_CHECKSUM_VERSION),
        FIELD_LIQUIBASE_CHECKSUM_VERSION);
    requireResourceReference(
        root.object(FIELD_SCHEMA_DEFINITION),
        SCHEMA_DEFINITION_NAME,
        EXPECTED_SCHEMA_DEFINITION_SHA_256);
    readExpectedResource(SCHEMA_DEFINITION_NAME, EXPECTED_SCHEMA_DEFINITION_SHA_256);
    String verifierSql =
        new String(
            readExpectedResource(VERIFIER_NAME, EXPECTED_VERIFIER_SHA_256),
            StandardCharsets.UTF_8);

    List<LegacySchemaContract> schemaContracts =
        root.objects(FIELD_SCHEMA_CONTRACTS).stream()
            .map(object -> parseSchemaContract(object, verifierSql))
            .toList();
    List<LegacyMigrationContract> migrations =
        root.objects(FIELD_MIGRATIONS).stream().map(this::parseMigration).toList();
    requireCanonicalPrefix(migrations, canonicalMigrations);
    return new LegacyLiquibaseContract(schemaContracts, migrations);
  }

  private LegacySchemaContract parseSchemaContract(
      StrictJsonObject object, String verifierSql) {
    object.requireFields(SCHEMA_CONTRACT_FIELDS, Set.of());
    requireResourceReference(
        object.object(FIELD_VERIFIER), VERIFIER_NAME, EXPECTED_VERIFIER_SHA_256);
    return new LegacySchemaContract(
        object.string(FIELD_PREFIX),
        object.strings(FIELD_MIGRATION_VERSIONS),
        verifierSql,
        object.objects(FIELD_EXPECTED_FINGERPRINTS).stream()
            .map(this::parseFingerprint)
            .toList());
  }

  private LegacySchemaFingerprint parseFingerprint(StrictJsonObject object) {
    object.requireFields(FINGERPRINT_FIELDS, Set.of());
    return new LegacySchemaFingerprint(
        object.integer(FIELD_POSTGRES_MAJOR), object.string(FIELD_SHA_256));
  }

  private LegacyMigrationContract parseMigration(StrictJsonObject object) {
    object.requireFields(MIGRATION_FIELDS, Set.of());
    return new LegacyMigrationContract(
        object.string(FIELD_VERSION),
        object.string(FIELD_CANONICAL_FILENAME),
        object.string(FIELD_CANONICAL_SHA_256),
        object.objects(FIELD_LEGACY_CHANGESETS).stream().map(this::parseChangeSet).toList());
  }

  private LegacyChangeSetContract parseChangeSet(StrictJsonObject object) {
    object.requireFields(CHANGESET_REQUIRED_FIELDS, CHANGESET_OPTIONAL_FIELDS);
    LegacyChecksumRule checksumRule =
        switch (object.string(FIELD_CHECKSUM_POLICY)) {
          case EXACT_CHECKSUM_POLICY ->
              new ExactLegacyChecksum(object.optionalString(FIELD_CHECKSUM));
          case SCHEMA_PARAMETERIZED_CHECKSUM_POLICY ->
              new SchemaParameterizedLegacyChecksum(
                  object.optionalString(FIELD_SCHEMA_PARAMETER));
          default -> throw failure("unknown checksum policy in trusted manifest");
        };
    return new LegacyChangeSetContract(
        enumValue(LegacySchemaMode.class, object.string(FIELD_SCHEMA_MODE)),
        object.string(FIELD_ID),
        object.string(FIELD_AUTHOR),
        object.string(FIELD_FILENAME),
        checksumRule,
        object.objects(FIELD_EXECUTION_POLICIES).stream()
            .map(this::parseExecutionPolicy)
            .toList());
  }

  private LegacyExecutionPolicy parseExecutionPolicy(StrictJsonObject object) {
    object.requireFields(EXECUTION_POLICY_FIELDS, Set.of());
    return new LegacyExecutionPolicy(
        enumValue(LegacyExecutionType.class, object.string(FIELD_EXECUTION_TYPE)),
        enumValue(LegacyAdoptionProof.class, object.string(FIELD_ADOPTION_PROOF)));
  }

  private void requireCanonicalPrefix(
      List<LegacyMigrationContract> contractMigrations,
      List<CanonicalMigration> canonicalMigrations) {
    if (contractMigrations.size() > canonicalMigrations.size()) {
      throw failure("legacy contract is newer than the canonical migration artifact");
    }
    for (int index = 0; index < contractMigrations.size(); index++) {
      LegacyMigrationContract contractMigration = contractMigrations.get(index);
      CanonicalMigration canonicalMigration = canonicalMigrations.get(index);
      requireEqual(
          contractMigration.canonicalFilename(),
          canonicalMigration.version(),
          FIELD_CANONICAL_FILENAME);
      requireEqual(
          contractMigration.canonicalSha256(),
          canonicalMigration.checksum(),
          FIELD_CANONICAL_SHA_256);
    }
  }

  private void requireResourceReference(
      StrictJsonObject object, String expectedName, String expectedSha256) {
    object.requireFields(RESOURCE_FIELDS, Set.of());
    requireEqual(expectedName, object.string(FIELD_NAME), FIELD_NAME);
    requireEqual(expectedSha256, object.string(FIELD_SHA_256), FIELD_SHA_256);
  }

  private byte[] readExpectedResource(String name, String expectedSha256) {
    byte[] contents = readUniqueResource(RESOURCE_DIRECTORY + name);
    requireChecksum(name, expectedSha256, contents);
    return contents;
  }

  private byte[] readUniqueResource(String name) {
    try {
      Enumeration<URL> resources = resourceLoader.getResources(name);
      if (!resources.hasMoreElements()) {
        throw failure("missing classpath resource " + name);
      }
      URL resource = resources.nextElement();
      if (resources.hasMoreElements()) {
        throw failure("duplicate classpath resource " + name);
      }
      try (InputStream stream = resource.openStream()) {
        return stream.readAllBytes();
      }
    } catch (IOException exception) {
      throw new InitializationFailureException(
          "read classpath resource " + name + ": " + exception.getMessage(), exception);
    }
  }

  private void requireChecksum(String name, String expected, byte[] contents) {
    String actual = SchemaContractDigest.sha256(contents);
    if (!expected.equals(actual)) {
      throw failure(
          "contract resource "
              + name
              + " checksum mismatch: expected="
              + expected
              + " actual="
              + actual);
    }
  }

  private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException exception) {
      throw new InitializationFailureException(
          "unknown " + type.getSimpleName() + " value " + value, exception);
    }
  }

  private void requireEqual(String expected, String actual, String field) {
    if (!expected.equals(actual)) {
      throw failure(field + " mismatch: expected=" + expected + " actual=" + actual);
    }
  }

  private void requireEqual(int expected, int actual, String field) {
    if (expected != actual) {
      throw failure(field + " mismatch: expected=" + expected + " actual=" + actual);
    }
  }

  private InitializationFailureException failure(String message) {
    return new InitializationFailureException(message);
  }
}
