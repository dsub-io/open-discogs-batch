package io.dsub.discogs.batch.dump.repository;

import static io.dsub.opendiscogs.jooq.tables.DiscogsDump.DISCOGS_DUMP;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpUrls;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.opendiscogs.jooq.tables.records.DiscogsDumpRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

/** Stores selected, checksum-pinned dump catalog rows in the canonical database. */
@Repository
@DependsOn("liquibase")
public class JooqDiscogsDumpRepository implements DiscogsDumpRepository {

  private static final int SHA_256_LENGTH = 64;
  private static final long UNKNOWN_SIZE_BYTES = 0L;
  private static final Pattern SHA_256_PATTERN =
      Pattern.compile("^[a-f\\d]{" + SHA_256_LENGTH + "}$", Pattern.CASE_INSENSITIVE);

  private final DSLContext context;

  public JooqDiscogsDumpRepository(DSLContext context) {
    if (context == null) {
      throw new IllegalArgumentException("DSL context cannot be null");
    }
    this.context = context;
  }

  @Override
  public List<DiscogsDump> findAllByLastModifiedAtIsBetween(LocalDate start, LocalDate end) {
    return context
        .selectFrom(DISCOGS_DUMP)
        .where(DISCOGS_DUMP.DUMP_DATE.ge(start).and(DISCOGS_DUMP.DUMP_DATE.lt(end)))
        .orderBy(DISCOGS_DUMP.DUMP_DATE, DISCOGS_DUMP.ID)
        .fetch(this::toDump);
  }

  @Override
  public List<DiscogsDump> findAll() {
    return context
        .selectFrom(DISCOGS_DUMP)
        .orderBy(DISCOGS_DUMP.DUMP_DATE, DISCOGS_DUMP.ID)
        .fetch(this::toDump);
  }

  @Override
  public int countItemsAfter(LocalDate start) {
    return context.fetchCount(
        context.selectFrom(DISCOGS_DUMP).where(DISCOGS_DUMP.DUMP_DATE.ge(start)));
  }

  @Override
  public int countItemsBefore(LocalDate end) {
    return context.fetchCount(
        context.selectFrom(DISCOGS_DUMP).where(DISCOGS_DUMP.DUMP_DATE.le(end)));
  }

  @Override
  public int countItemsBetween(LocalDate start, LocalDate end) {
    return context.fetchCount(
        context
            .selectFrom(DISCOGS_DUMP)
            .where(DISCOGS_DUMP.DUMP_DATE.ge(start).and(DISCOGS_DUMP.DUMP_DATE.lt(end))));
  }

  @Override
  public List<DiscogsDump> findByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end) {
    return context
        .selectFrom(DISCOGS_DUMP)
        .where(
            DISCOGS_DUMP
                .ENTITY_TYPE
                .eq(type.toString())
                .and(DISCOGS_DUMP.DUMP_DATE.ge(start))
                .and(DISCOGS_DUMP.DUMP_DATE.lt(end)))
        .orderBy(DISCOGS_DUMP.DUMP_DATE, DISCOGS_DUMP.ID)
        .fetch(this::toDump);
  }

  @Override
  public DiscogsDump findTopByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end) {
    return context
        .selectFrom(DISCOGS_DUMP)
        .where(
            DISCOGS_DUMP
                .ENTITY_TYPE
                .eq(type.toString())
                .and(DISCOGS_DUMP.DUMP_DATE.ge(start))
                .and(DISCOGS_DUMP.DUMP_DATE.lt(end)))
        .orderBy(
            DISCOGS_DUMP.DUMP_DATE.sort(SortOrder.DESC),
            DISCOGS_DUMP.ID.sort(SortOrder.DESC))
        .limit(1)
        .fetchOne(this::toDump);
  }

  @Override
  public DiscogsDump findTopByType(EntityType type) {
    return context
        .selectFrom(DISCOGS_DUMP)
        .where(DISCOGS_DUMP.ENTITY_TYPE.eq(type.toString()))
        .orderBy(
            DISCOGS_DUMP.DUMP_DATE.sort(SortOrder.DESC),
            DISCOGS_DUMP.ID.sort(SortOrder.DESC))
        .limit(1)
        .fetchOne(this::toDump);
  }

  @Override
  public DiscogsDump findByETag(String eTag) {
    if (eTag == null || eTag.isBlank()) {
      return null;
    }
    return context
        .selectFrom(DISCOGS_DUMP)
        .where(DISCOGS_DUMP.ETAG.eq(eTag))
        .orderBy(
            DISCOGS_DUMP.DUMP_DATE.sort(SortOrder.DESC),
            DISCOGS_DUMP.ID.sort(SortOrder.DESC))
        .limit(1)
        .fetchOne(this::toDump);
  }

  @Override
  public boolean existsByETag(String eTag) {
    return eTag != null
        && !eTag.isBlank()
        && context.fetchExists(
            context.selectOne().from(DISCOGS_DUMP).where(DISCOGS_DUMP.ETAG.eq(eTag)));
  }

  @Override
  public int count() {
    return context.fetchCount(DISCOGS_DUMP);
  }

  @Override
  public void saveAll(Collection<DiscogsDump> dumps) {
    if (dumps == null || dumps.isEmpty()) {
      return;
    }
    List<DiscogsDump> validated = dumps.stream().map(this::requirePinnedDump).toList();
    context.transaction(
        configuration -> {
          DSLContext transaction = DSL.using(configuration);
          for (DiscogsDump dump : validated) {
            transaction
                .insertInto(
                    DISCOGS_DUMP,
                    DISCOGS_DUMP.ETAG,
                    DISCOGS_DUMP.DUMP_DATE,
                    DISCOGS_DUMP.ENTITY_TYPE,
                    DISCOGS_DUMP.CHECKSUM_SHA256,
                    DISCOGS_DUMP.SIZE_BYTES,
                    DISCOGS_DUMP.URI)
                .values(
                    dump.getETag(),
                    dump.getLastModifiedAt(),
                    dump.getType().toString(),
                    dump.getChecksumSha256().toLowerCase(java.util.Locale.ROOT),
                    normalizedSize(dump),
                    dump.getUriString())
                .onConflict(
                    DISCOGS_DUMP.DUMP_DATE,
                    DISCOGS_DUMP.ENTITY_TYPE,
                    DISCOGS_DUMP.CHECKSUM_SHA256)
                .doNothing()
                .execute();
          }
        });
  }

  @Override
  public void deleteAll() {
    context.deleteFrom(DISCOGS_DUMP).execute();
  }

  @Override
  public void save(DiscogsDump dump) {
    requirePinnedDump(dump);
    saveAll(List.of(dump));
  }

  private DiscogsDump requirePinnedDump(DiscogsDump dump) {
    if (dump == null) {
      throw new IllegalArgumentException("dump cannot be null");
    }
    if (dump.getETag() == null || dump.getETag().isBlank()) {
      throw new IllegalArgumentException("dump ETag cannot be blank");
    }
    if (dump.getUriString() == null || dump.getUriString().isBlank()) {
      throw new IllegalArgumentException("dump URI cannot be blank");
    }
    if (dump.getType() == null) {
      throw new IllegalArgumentException("dump entity type cannot be null");
    }
    if (dump.getLastModifiedAt() == null) {
      throw new IllegalArgumentException("dump date cannot be null");
    }
    String checksum = dump.getChecksumSha256();
    if (checksum == null || !SHA_256_PATTERN.matcher(checksum).matches()) {
      throw new IllegalArgumentException("dump must have a valid pinned SHA-256 checksum");
    }
    return dump;
  }

  private long normalizedSize(DiscogsDump dump) {
    return dump.getSize() == null || dump.getSize() < 0
        ? UNKNOWN_SIZE_BYTES
        : dump.getSize();
  }

  private DiscogsDump toDump(DiscogsDumpRecord record) {
    return new DiscogsDump(
        record.getEtag(),
        EntityType.of(record.getEntityType()),
        record.getUri(),
        record.getSizeBytes(),
        record.getDumpDate(),
        DiscogsDumpUrls.dump(DiscogsDumpUrls.PUBLIC_CATALOG_URI, record.getUri()),
        DiscogsDumpUrls.manifest(DiscogsDumpUrls.PUBLIC_CATALOG_URI, record.getDumpDate()),
        record.getChecksumSha256());
  }
}
