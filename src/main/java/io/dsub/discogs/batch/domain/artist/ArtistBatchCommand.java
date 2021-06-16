package io.dsub.discogs.batch.domain.artist;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.artist.ArtistAlias;
import io.dsub.discogs.common.entity.artist.ArtistGroup;
import io.dsub.discogs.common.entity.artist.ArtistMember;
import io.dsub.discogs.common.entity.artist.ArtistNameVariation;
import io.dsub.discogs.common.entity.artist.ArtistUrl;
import io.dsub.discogs.common.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch commands for {@link io.dsub.discogs.common.entity.artist} package. This class is NOT
 * instantiatable.
 */
public abstract class ArtistBatchCommand {

  /** Private constructor to prevent instantiation. */
  private ArtistBatchCommand() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistCommand implements BatchCommand {

    private Long id;
    private String name;
    private String realName;
    private String profile;
    private String dataQuality;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return Artist.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistMemberCommand implements BatchCommand {

    private Long artist;
    private Long member;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistMember.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistGroupCommand implements BatchCommand {

    private Long artist;
    private Long group;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistGroup.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistAliasCommand implements BatchCommand {

    private Long artist;
    private Long alias;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistAlias.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistUrlCommand implements BatchCommand {

    private Long artist;
    private String url;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistUrl.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ArtistNameVariationCommand implements BatchCommand {

    private Long artist;
    private String name;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistNameVariation.class;
    }
  }
}
