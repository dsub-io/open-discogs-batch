package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

/**
 * Owns the database-wide import admission, history, and entity advisory locks for one process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportExecutionCoordinator {

  private static final String PROCESSOR = "open-discogs-batch";

  private final DataSource dataSource;

  private Connection lockConnection;
  private List<Integer> acquiredLockKeys = List.of();
  private Long activeRunId;

  public synchronized Preparation prepare(JobParameters parameters)
      throws ImportExecutionException {
    if (lockConnection != null) {
      throw new ImportExecutionException("import execution has already been prepared");
    }

    String manifestSha256 = requiredParameter(parameters, ImportJobParameters.MANIFEST_SHA256);
    boolean force = booleanParameter(parameters, ImportJobParameters.FORCE);
    boolean allowDowngrade =
        booleanParameter(parameters, ImportJobParameters.ALLOW_DOWNGRADE);
    List<PlannedDump> dumps = plannedDumps(parameters);
    List<String> entityTypes =
        ImportExecution.orderedEntityTypes(
            dumps.stream().map(PlannedDump::entityType).toList());

    try {
      lockConnection = dataSource.getConnection();
      lockConnection.setAutoCommit(true);
      acquireEntityLocks(entityTypes);
      lockConnection.setAutoCommit(false);

      markAbandonedRuns(entityTypes);
      assertNotDowngrade(dumps, allowDowngrade);

      Long successfulRunId = findSuccessfulRun(manifestSha256);
      if (successfulRunId != null && !force) {
        lockConnection.commit();
        log.info(
            "manifest {} already succeeded as import run {}. skipping.",
            manifestSha256,
            successfulRunId);
        releaseEntityLocks();
        return Preparation.skipped(manifestSha256, successfulRunId);
      }

      List<Long> dumpIds = new ArrayList<>(dumps.size());
      for (PlannedDump dump : dumps) {
        dumpIds.add(findOrInsertDump(dump));
      }
      activeRunId =
          insertRun(manifestSha256, force, allowDowngrade, processorVersion());
      for (int index = 0; index < dumps.size(); index++) {
        insertRunDump(activeRunId, dumps.get(index).entityType(), dumpIds.get(index));
      }
      lockConnection.commit();
      log.info(
          "started import run {} for manifest {} and entities {}",
          activeRunId,
          manifestSha256,
          entityTypes);
      return Preparation.started(manifestSha256, activeRunId);
    } catch (ImportExecutionException exception) {
      rollbackAndRelease();
      throw exception;
    } catch (Exception exception) {
      rollbackAndRelease();
      throw new ImportExecutionException("failed to prepare import execution", exception);
    }
  }

  public synchronized void complete(boolean success, Throwable failure)
      throws ImportExecutionException {
    if (activeRunId == null || lockConnection == null) {
      return;
    }

    try {
      lockConnection.setAutoCommit(false);
      try (PreparedStatement statement =
          lockConnection.prepareStatement(
              """
              update discogs_import_run
              set status = ?,
                  completed_at = now(),
                  failure_message = ?
              where id = ?
                and status = 'running'
              """)) {
        statement.setString(1, success ? "success" : "failed");
        if (success || failure == null) {
          statement.setNull(2, java.sql.Types.VARCHAR);
        } else {
          statement.setString(2, failure.toString());
        }
        statement.setLong(3, activeRunId);
        if (statement.executeUpdate() != 1) {
          throw new ImportExecutionException(
              "active import run was not in running state: " + activeRunId);
        }
      }
      lockConnection.commit();
      log.info("completed import run {} with status {}", activeRunId, success ? "success" : "failed");
    } catch (ImportExecutionException exception) {
      rollbackQuietly();
      throw exception;
    } catch (SQLException exception) {
      rollbackQuietly();
      throw new ImportExecutionException("failed to complete import execution", exception);
    } finally {
      activeRunId = null;
      releaseEntityLocks();
    }
  }

  private List<PlannedDump> plannedDumps(JobParameters parameters)
      throws ImportExecutionException {
    List<PlannedDump> dumps = new ArrayList<>();
    for (EntityType type : EntityType.values()) {
      if (parameters.getParameter(type.toString()) == null) {
        continue;
      }
      String sizeValue = requiredParameter(parameters, ImportJobParameters.size(type));
      try {
        dumps.add(
            new PlannedDump(
                type.toString(),
                LocalDate.parse(requiredParameter(parameters, ImportJobParameters.date(type))),
                requiredParameter(parameters, ImportJobParameters.checksum(type)),
                Long.parseLong(sizeValue),
                requiredParameter(parameters, ImportJobParameters.etag(type)),
                requiredParameter(parameters, ImportJobParameters.uri(type))));
      } catch (RuntimeException exception) {
        throw new ImportExecutionException(
            "invalid import parameters for " + type, exception);
      }
    }
    if (dumps.isEmpty()) {
      throw new ImportExecutionException("import plan must contain at least one entity type");
    }
    return List.copyOf(dumps);
  }

  private String requiredParameter(JobParameters parameters, String key)
      throws ImportExecutionException {
    String value = parameters.getString(key);
    if (value == null || value.isBlank()) {
      throw new ImportExecutionException("missing import job parameter: " + key);
    }
    return value;
  }

  private boolean booleanParameter(JobParameters parameters, String key) {
    return Boolean.parseBoolean(parameters.getString(key, "false"));
  }

  private void acquireEntityLocks(List<String> entityTypes)
      throws SQLException, ImportExecutionException {
    List<Integer> acquired = new ArrayList<>(entityTypes.size());
    try (PreparedStatement statement =
        lockConnection.prepareStatement("select pg_try_advisory_lock(?, ?)")) {
      for (String entityType : entityTypes) {
        int entityKey = ImportExecution.entityLockKey(entityType);
        statement.setInt(1, ImportExecution.ADVISORY_LOCK_NAMESPACE);
        statement.setInt(2, entityKey);
        try (ResultSet result = statement.executeQuery()) {
          result.next();
          if (!result.getBoolean(1)) {
            acquiredLockKeys = List.copyOf(acquired);
            throw new ImportExecutionException(
                "another import is already updating " + entityType);
          }
        }
        acquired.add(entityKey);
      }
    }
    acquiredLockKeys = List.copyOf(acquired);
  }

  private void markAbandonedRuns(List<String> entityTypes) throws SQLException {
    Array requestedTypes =
        lockConnection.createArrayOf("varchar", entityTypes.toArray());
    try (PreparedStatement statement =
            lockConnection.prepareStatement(
                """
                update discogs_import_run import_run
                set status = 'failed',
                    completed_at = now(),
                    failure_message = 'recovered after entity advisory locks were released'
                where import_run.status = 'running'
                  and exists (
                    select 1
                    from discogs_import_run_dump run_dump
                    where run_dump.import_run_id = import_run.id
                      and run_dump.entity_type = any (?)
                  )
                """)) {
      statement.setArray(1, requestedTypes);
      statement.executeUpdate();
    } finally {
      requestedTypes.free();
    }
  }

  private void assertNotDowngrade(List<PlannedDump> dumps, boolean allowDowngrade)
      throws SQLException, ImportExecutionException {
    if (allowDowngrade) {
      return;
    }
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            select dump_date
            from discogs_import_checkpoint
            where entity_type = ?
            """)) {
      for (PlannedDump dump : dumps) {
        statement.setString(1, dump.entityType());
        try (ResultSet result = statement.executeQuery()) {
          if (result.next()
              && ImportExecution.isDowngrade(
                  dump.dumpDate(), result.getDate(1).toLocalDate())) {
            throw new ImportExecutionException(
                "dump "
                    + dump.entityType()
                    + " "
                    + dump.dumpDate()
                    + " predates checkpoint "
                    + result.getDate(1).toLocalDate()
                    + "; use --allow-downgrade to override");
          }
        }
      }
    }
  }

  private Long findSuccessfulRun(String manifestSha256) throws SQLException {
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            select id
            from discogs_import_run
            where manifest_sha256 = ?
              and status = 'success'
            order by completed_at desc, id desc
            limit 1
            """)) {
      statement.setString(1, manifestSha256);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? result.getLong(1) : null;
      }
    }
  }

  private long findOrInsertDump(PlannedDump dump) throws SQLException {
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            insert into discogs_dump
                (etag, dump_date, entity_type, checksum_sha256, size_bytes, uri)
            values (?, ?, ?, ?, ?, ?)
            on conflict (dump_date, entity_type, checksum_sha256) do nothing
            returning id
            """)) {
      statement.setString(1, dump.etag());
      statement.setDate(2, Date.valueOf(dump.dumpDate()));
      statement.setString(3, dump.entityType());
      statement.setString(4, dump.checksumSha256());
      statement.setLong(5, dump.sizeBytes());
      statement.setString(6, dump.uri());
      try (ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          return result.getLong(1);
        }
      }
    }
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            select id
            from discogs_dump
            where dump_date = ?
              and entity_type = ?
              and checksum_sha256 = ?
            """)) {
      statement.setDate(1, Date.valueOf(dump.dumpDate()));
      statement.setString(2, dump.entityType());
      statement.setString(3, dump.checksumSha256());
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("failed to resolve inserted dump " + dump.entityType());
        }
        return result.getLong(1);
      }
    }
  }

  private long insertRun(
      String manifestSha256,
      boolean force,
      boolean allowDowngrade,
      String version)
      throws SQLException {
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            insert into discogs_import_run
                (manifest_sha256, status, force_requested,
                 allow_downgrade_requested, processor, processor_version)
            values (?, 'running', ?, ?, ?, ?)
            """,
            Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, manifestSha256);
      statement.setBoolean(2, force);
      statement.setBoolean(3, allowDowngrade);
      statement.setString(4, PROCESSOR);
      statement.setString(5, version);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new SQLException("import run insert did not return an ID");
        }
        return keys.getLong(1);
      }
    }
  }

  private void insertRunDump(long runId, String entityType, long dumpId)
      throws SQLException {
    try (PreparedStatement statement =
        lockConnection.prepareStatement(
            """
            insert into discogs_import_run_dump
                (import_run_id, entity_type, dump_id)
            values (?, ?, ?)
            """)) {
      statement.setLong(1, runId);
      statement.setString(2, entityType);
      statement.setLong(3, dumpId);
      statement.executeUpdate();
    }
  }

  private String processorVersion() {
    String version = ImportExecutionCoordinator.class.getPackage().getImplementationVersion();
    return version == null || version.isBlank() ? "development" : version;
  }

  private void rollbackAndRelease() {
    rollbackQuietly();
    activeRunId = null;
    releaseEntityLocks();
  }

  private void rollbackQuietly() {
    if (lockConnection == null) {
      return;
    }
    try {
      lockConnection.rollback();
    } catch (SQLException exception) {
      log.warn("failed to roll back import execution", exception);
    }
  }

  private void releaseEntityLocks() {
    if (lockConnection == null) {
      return;
    }
    List<Integer> reverse = new ArrayList<>(acquiredLockKeys);
    Collections.reverse(reverse);
    try (PreparedStatement statement =
        lockConnection.prepareStatement("select pg_advisory_unlock(?, ?)")) {
      for (int entityKey : reverse) {
        statement.setInt(1, ImportExecution.ADVISORY_LOCK_NAMESPACE);
        statement.setInt(2, entityKey);
        statement.execute();
      }
    } catch (SQLException exception) {
      log.warn("failed to release an import entity lock", exception);
    } finally {
      try {
        lockConnection.close();
      } catch (SQLException exception) {
        log.warn("failed to close the import lock connection", exception);
      }
      lockConnection = null;
      acquiredLockKeys = List.of();
    }
  }

  public record Preparation(
      boolean skipped, String manifestSha256, Long runId, Long priorSuccessfulRunId) {

    public static Preparation skipped(String manifestSha256, long successfulRunId) {
      return new Preparation(true, manifestSha256, null, successfulRunId);
    }

    public static Preparation started(String manifestSha256, long runId) {
      return new Preparation(false, manifestSha256, runId, null);
    }
  }

  private record PlannedDump(
      String entityType,
      LocalDate dumpDate,
      String checksumSha256,
      long sizeBytes,
      String etag,
      String uri) {
  }
}
