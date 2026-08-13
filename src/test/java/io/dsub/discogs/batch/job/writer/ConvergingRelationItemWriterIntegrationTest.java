package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.domain.CanonicalRelationIdentity;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.ArtistNameVariationRecord;
import io.dsub.opendiscogs.jooq.tables.records.LabelUrlRecord;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

class ConvergingRelationItemWriterIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final int BATCH_SIZE = 5;
  private static final int KNOWN_COLLISION_ARTIST_ID = 33_476;
  private static final int KNOWN_COLLISION_HASH = -1_130_078_775;
  private static final String DELETE_GUARD_FUNCTION = "reject_new_relation_root_delete";
  private static final String DELETE_GUARD_TRIGGER = "guard_new_relation_root_delete";

  private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

  @BeforeEach
  void clearState() {
    jdbcTemplate.execute("truncate table artist, label restart identity cascade");
    jdbcTemplate.update(
        """
        insert into label (id, created_at, last_modified_at, name)
        values (1, now(), now(), 'label')
        """);
  }

  @Test
  void keepsCurrentCanonicalRowAndDeletesOnlyStaleKeys() throws Exception {
    String currentUrl = "https://current.example";
    String staleUrl = "https://stale.example";
    LocalDateTime existingObservedAt = LocalDateTime.of(2026, 8, 1, 0, 0);
    jdbcTemplate.update(
        """
        insert into label_url
            (last_modified_at, hash, identity_sha256, url, label_id)
        values
            (?, ?, ?, ?, 1),
            (?, ?, ?, ?, 1)
        """,
        existingObservedAt,
        currentUrl.hashCode(),
        CanonicalRelationIdentity.digest(
            CanonicalRelationIdentity.Relation.LABEL_URL, currentUrl),
        currentUrl,
        existingObservedAt,
        staleUrl.hashCode(),
        CanonicalRelationIdentity.digest(
            CanonicalRelationIdentity.Relation.LABEL_URL, staleUrl),
        staleUrl);

    writer().write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.LABEL,
                    1,
                    List.of(labelUrl(1, currentUrl))))));

    assertThat(
            jdbcTemplate.queryForList(
                "select url from label_url", String.class))
        .containsExactly(currentUrl);
    assertThat(
            jdbcTemplate.queryForObject(
                "select last_modified_at from label_url", LocalDateTime.class))
        .isEqualTo(existingObservedAt);
  }

  @Test
  void replacesTouchedLegacyNullIdentityWithCanonicalDigest() throws Exception {
    String url = "https://legacy.example";
    jdbcTemplate.update(
        """
        insert into label_url (last_modified_at, hash, identity_sha256, url, label_id)
        values (now(), ?, null, ?, 1)
        """,
        url.hashCode(),
        url);

    writer().write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.LABEL,
                    1,
                    List.of(labelUrl(1, url))))));

    assertThat(
            jdbcTemplate.queryForObject(
                "select identity_sha256 from label_url where label_id = 1",
                byte[].class))
        .containsExactly(
            CanonicalRelationIdentity.digest(
                CanonicalRelationIdentity.Relation.LABEL_URL, url));
  }

  @Test
  void preservesTheKnownArtist33476DistinctPayloadHashCollisionAndRetriesIdempotently()
      throws Exception {
    jdbcTemplate.update(
        """
        insert into artist (id, created_at, last_modified_at, name)
        values (?, now(), now(), 'Linval Thompson')
        """,
        KNOWN_COLLISION_ARTIST_ID);
    writer().write(new Chunk<>(List.of(artistCollision())));
    List<String> firstState =
        jdbcTemplate.queryForList(
            """
            select name_variation || ':' || hash || ':' || encode(identity_sha256, 'hex')
            from artist_name_variation
            where artist_id = ?
            order by name_variation
            """,
            String.class,
            KNOWN_COLLISION_ARTIST_ID);

    writer().write(new Chunk<>(List.of(artistCollision())));

    assertThat(firstState).hasSize(2).doesNotHaveDuplicates();
    assertThat(
            jdbcTemplate.queryForList(
                """
                select name_variation || ':' || hash || ':' || encode(identity_sha256, 'hex')
                from artist_name_variation
                where artist_id = ?
                order by name_variation
                """,
                String.class,
                KNOWN_COLLISION_ARTIST_ID))
        .isEqualTo(firstState);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(distinct hash)
                from artist_name_variation
                where artist_id = ?
                """,
                Long.class,
                KNOWN_COLLISION_ARTIST_ID))
        .isEqualTo(2L);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from artist_name_variation
                where artist_id = ? and hash = ?
                """,
                Long.class,
                KNOWN_COLLISION_ARTIST_ID,
                KNOWN_COLLISION_HASH))
        .isOne();
  }

  @Test
  void emptyRelationSetDeletesEveryPriorRelationForTheRoot() throws Exception {
    jdbcTemplate.update(
        """
        insert into label_url
            (last_modified_at, hash, url, label_id)
        values (now(), 1, 'https://stale.example', 1)
        """);

    writer().write(
        new Chunk<>(List.of(new RelationSet(EntityType.LABEL, 1, List.of()))));

    assertThat(jdbcTemplate.queryForObject("select count(*) from label_url", Long.class))
        .isZero();
  }

  @Test
  void newRelationRootDoesNotIssueAStaleDelete() throws Exception {
    String url = "https://new.example";
    jdbcTemplate.execute(
        """
        create function reject_new_relation_root_delete() returns trigger
        language plpgsql as $$
        begin
          raise exception 'new relation root must not issue delete';
        end;
        $$
        """);
    jdbcTemplate.execute(
        """
        create trigger guard_new_relation_root_delete
        before delete on label_url
        for each statement execute function reject_new_relation_root_delete()
        """);

    try {
      writer().write(
          new Chunk<>(
              List.of(
                  new RelationSet(
                      EntityType.LABEL,
                      1,
                      List.of(labelUrl(1, url))))));
    } finally {
      jdbcTemplate.execute("drop trigger " + DELETE_GUARD_TRIGGER + " on label_url");
      jdbcTemplate.execute("drop function " + DELETE_GUARD_FUNCTION + "()");
    }

    assertThat(jdbcTemplate.queryForObject("select count(*) from label_url", Long.class))
        .isOne();
  }

  private ItemWriter<RelationSet> writer() {
    DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
    ItemWriter<TableRecord<?>> records = new DefaultLJooqItemWriter<>(context);
    ItemWriter<Collection<TableRecord<?>>> batches =
        new CollectionItemWriter<>(records, BATCH_SIZE);
    return new ConvergingRelationItemWriter(dataSource, batches);
  }

  private LabelUrlRecord labelUrl(int labelId, String url) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setUrl(url)
        .setHash(url.hashCode())
        .setLastModifiedAt(now);
  }

  private ArtistNameVariationRecord nameVariation(String value) {
    return new ArtistNameVariationRecord()
        .setArtistId(KNOWN_COLLISION_ARTIST_ID)
        .setHash(KNOWN_COLLISION_HASH)
        .setNameVariation(value)
        .setLastModifiedAt(LocalDateTime.now(ZoneOffset.UTC));
  }

  private RelationSet artistCollision() {
    return new RelationSet(
        EntityType.ARTIST,
        KNOWN_COLLISION_ARTIST_ID,
        List.of(nameVariation("Al Thompson"), nameVariation("C. Thompson")));
  }
}
