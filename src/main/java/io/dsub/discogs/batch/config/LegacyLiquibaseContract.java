package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.util.List;

/** Typed model of the immutable legacy Liquibase adoption contract. */
record LegacyLiquibaseContract(
    List<LegacySchemaContract> schemaContracts,
    List<LegacyMigrationContract> migrations) {

  LegacySchemaContract schemaContractForPrefix(String prefix) {
    return schemaContracts.stream()
        .filter(contract -> contract.prefix().equals(prefix))
        .findFirst()
        .orElseThrow(
            () -> new InitializationFailureException(
                "legacy Liquibase history has unsupported prefix " + prefix));
  }

  LegacySchemaContract schemaContractForLength(int migrationCount) {
    return schemaContracts.stream()
        .filter(contract -> contract.migrationVersions().size() == migrationCount)
        .findFirst()
        .orElseThrow(
            () -> new InitializationFailureException(
                "legacy Liquibase history is not an exact supported prefix: row-count="
                    + migrationCount));
  }
}

record LegacySchemaContract(
    String prefix,
    List<String> migrationVersions,
    String verifierSql,
    List<LegacySchemaFingerprint> expectedFingerprints) {

  String fingerprintForPostgres(int postgresMajor) {
    return expectedFingerprints.stream()
        .filter(fingerprint -> fingerprint.postgresMajor() == postgresMajor)
        .map(LegacySchemaFingerprint::sha256)
        .findFirst()
        .orElseThrow(
            () -> new InitializationFailureException(
                "legacy schema contract "
                    + prefix
                    + " does not support PostgreSQL "
                    + postgresMajor));
  }
}

record LegacySchemaFingerprint(int postgresMajor, String sha256) {}

record LegacyMigrationContract(
    String version,
    String canonicalFilename,
    String canonicalSha256,
    List<LegacyChangeSetContract> legacyChangeSets) {

  LegacyChangeSetContract changeSetFor(LegacySchemaMode schemaMode) {
    return legacyChangeSets.stream()
        .filter(changeSet -> changeSet.schemaMode() == schemaMode)
        .findFirst()
        .orElseThrow(
            () -> new InitializationFailureException(
                "legacy migration " + version + " has no " + schemaMode + " changeset"));
  }
}

record LegacyChangeSetContract(
    LegacySchemaMode schemaMode,
    String id,
    String author,
    String filename,
    LegacyChecksumRule checksumRule,
    List<LegacyExecutionPolicy> executionPolicies) {

  LegacyExecutionPolicy policyFor(LegacyExecutionType executionType) {
    return executionPolicies.stream()
        .filter(policy -> policy.executionType() == executionType)
        .findFirst()
        .orElseThrow(
            () -> new InitializationFailureException(
                "legacy changeset " + id + " does not permit " + executionType));
  }
}

sealed interface LegacyChecksumRule
    permits ExactLegacyChecksum, SchemaParameterizedLegacyChecksum {}

record ExactLegacyChecksum(String checksum) implements LegacyChecksumRule {}

record SchemaParameterizedLegacyChecksum(String schemaParameter)
    implements LegacyChecksumRule {}

record LegacyExecutionPolicy(
    LegacyExecutionType executionType, LegacyAdoptionProof adoptionProof) {}

enum LegacySchemaMode {
  PUBLIC,
  CUSTOM
}

enum LegacyExecutionType {
  EXECUTED,
  MARK_RAN
}

enum LegacyAdoptionProof {
  EXACT_CHECKSUM,
  SCHEMA_CONTRACT
}
