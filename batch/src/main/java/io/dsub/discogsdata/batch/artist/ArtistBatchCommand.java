package io.dsub.discogsdata.batch.artist;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import io.dsub.discogsdata.common.entity.artist.ArtistNameVariation;
import io.dsub.discogsdata.common.entity.artist.ArtistUrl;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch commands for {@link io.dsub.discogsdata.common.entity.artist} package. This class is NOT
 * instantiatable.
 */
public abstract class ArtistBatchCommand {

  /**
   * Private constructor to prevent instantiation.
   */
  private ArtistBatchCommand() {
  }

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
    private LocalDateTime lastModifiedAt;

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
    private LocalDateTime lastModifiedAt;

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
    private LocalDateTime lastModifiedAt;

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
    private LocalDateTime lastModifiedAt;

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
    private LocalDateTime lastModifiedAt;

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
    private LocalDateTime lastModifiedAt;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ArtistNameVariation.class;
    }
  }

  ;
}