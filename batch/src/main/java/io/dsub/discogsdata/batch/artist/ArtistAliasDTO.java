package io.dsub.discogsdata.batch.artist;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArtistAliasDTO {

  private final Long id;
  private final Long artist;
  private final Long alias;
}