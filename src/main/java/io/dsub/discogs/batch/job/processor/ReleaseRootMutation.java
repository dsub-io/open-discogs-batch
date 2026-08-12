package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseAssignment;
import io.dsub.opendiscogs.jooq.tables.records.GenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.StyleRecord;
import java.util.List;

/** Complete canonical state produced from one Release root element. */
public record ReleaseRootMutation(
    ReleaseItemRecord root,
    List<GenreRecord> genres,
    List<StyleRecord> styles,
    RelationSet relations,
    MasterMainReleaseAssignment mainReleaseAssignment) {

  public ReleaseRootMutation {
    if (root == null || relations == null || mainReleaseAssignment == null) {
      throw new IllegalArgumentException("release root mutation fields must not be null");
    }
    genres = List.copyOf(genres);
    styles = List.copyOf(styles);
  }
}
