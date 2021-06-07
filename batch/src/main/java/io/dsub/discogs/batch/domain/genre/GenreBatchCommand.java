package io.dsub.discogs.batch.domain.genre;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.common.entity.Genre;
import io.dsub.discogs.common.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch command for {@link Genre}. This class is NOT
 * instantiatable.
 */
public abstract class GenreBatchCommand {

  /**
   * Private constructor to prevent instantiation.
   */
  private GenreBatchCommand() {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class GenreCommand implements BatchCommand {

    private String name;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return Genre.class;
    }
  }
}
