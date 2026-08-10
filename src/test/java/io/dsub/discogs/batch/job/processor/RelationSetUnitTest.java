package io.dsub.discogs.batch.job.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.opendiscogs.jooq.tables.records.ArtistUrlRecord;
import java.util.ArrayList;
import java.util.List;
import org.jooq.UpdatableRecord;
import org.junit.jupiter.api.Test;

class RelationSetUnitTest {

  @Test
  void validatesIdentityAndCopiesRecords() {
    List<UpdatableRecord<?>> records = new ArrayList<>();
    records.add(new ArtistUrlRecord());
    RelationSet relationSet = new RelationSet(EntityType.ARTIST, 1, records);
    records.clear();

    assertThat(relationSet.records()).hasSize(1);
    assertThatThrownBy(() -> new RelationSet(null, 1, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("entity type");
    assertThatThrownBy(() -> new RelationSet(EntityType.ARTIST, 0, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("root ID");
    assertThatThrownBy(() -> new RelationSet(EntityType.ARTIST, 1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("records");
  }
}
