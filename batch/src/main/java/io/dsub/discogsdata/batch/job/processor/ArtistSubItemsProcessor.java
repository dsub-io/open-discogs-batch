package io.dsub.discogsdata.batch.job.processor;

import io.dsub.discogsdata.batch.artist.ArtistXML;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import io.dsub.discogsdata.common.entity.artist.ArtistNameVariation;
import io.dsub.discogsdata.common.entity.artist.ArtistUrl;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class ArtistSubItemsProcessor implements ItemProcessor<ArtistXML, Collection<BaseEntity>> {

  @Override
  public Collection<BaseEntity> process(ArtistXML item) {
    List<BaseEntity> entityList = new ArrayList<>();

    Artist artistRef = getArtistById(item.getId());

    if (isPresent(item.getUrls())) {
      item.getUrls().stream()
          .map(url -> new ArtistUrl().withArtist(artistRef).withUrl(url))
          .forEach(entityList::add);
    }

    if (isPresent(item.getNameVariations())) {
      item.getNameVariations().stream()
          .map(
              nameVar -> new ArtistNameVariation()
                  .withArtist(artistRef)
                  .withName(nameVar))
          .forEach(entityList::add);
    }

    if (isPresent(item.getAliases())) {
      item.getAliases().stream()
          .map(alias -> getArtistById(alias.getId()))
          .map(
              alias -> new ArtistAlias()
                  .withArtist(artistRef)
                  .withAlias(alias))
          .forEach(entityList::add);
    }

    if (isPresent(item.getGroups())) {
      item.getGroups().stream()
          .map(group -> getArtistById(group.getId()))
          .map(
              group -> new ArtistGroup()
                  .withArtist(artistRef)
                  .withGroup(group))
          .forEach(entityList::add);
    }

    if (isPresent(item.getMembers())) {
      item.getMembers().stream()
          .map(member -> getArtistById(member.getId()))
          .map(
              member -> new ArtistMember()
                  .withArtist(artistRef)
                  .withMember(member))
          .forEach(entityList::add);
    }

    return entityList;
  }

  // TODO: implement
  private Artist getArtistById(long id) {
    return null;
  }

  private boolean isPresent(Collection<?> items) {
    return (items != null && !items.isEmpty());
  }
}
