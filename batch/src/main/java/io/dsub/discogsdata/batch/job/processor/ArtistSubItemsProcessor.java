package io.dsub.discogsdata.batch.job.processor;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistBatchCommand.ArtistAliasCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistBatchCommand.ArtistGroupCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistBatchCommand.ArtistMemberCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistBatchCommand.ArtistNameVariationCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistBatchCommand.ArtistUrlCommand;
import io.dsub.discogsdata.batch.domain.artist.ArtistXML;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class ArtistSubItemsProcessor implements ItemProcessor<ArtistXML, Collection<BatchCommand>> {

  @Override
  public Collection<BatchCommand> process(ArtistXML item) {
    long artistId = item.getId();

    List<BatchCommand> batchCommands = new LinkedList<>();

    if (item.getAliases() != null) {
      item.getAliases().stream()
          .map(alias -> ArtistAliasCommand.builder()
              .alias(alias.getId())
              .artist(artistId)
              .build())
          .forEach(batchCommands::add);
    }

    if (item.getGroups() != null) {
      item.getGroups().stream()
          .map(group -> ArtistGroupCommand.builder()
              .artist(artistId)
              .group(group.getId())
              .build())
          .forEach(batchCommands::add);
    }

    if (item.getMembers() != null) {
      item.getMembers().stream()
          .map(member -> ArtistMemberCommand.builder()
              .artist(artistId)
              .member(member.getId())
              .build())
          .forEach(batchCommands::add);
    }

    if (item.getUrls() != null) {
      item.getUrls().stream()
          .map(url -> ArtistUrlCommand.builder()
              .artist(artistId)
              .url(url)
              .build())
          .forEach(batchCommands::add);
    }

    if (item.getNameVariations() != null) {
      item.getNameVariations().stream()
          .map(name -> ArtistNameVariationCommand.builder()
              .artist(artistId)
              .name(name)
              .build())
          .forEach(batchCommands::add);
    }
    return batchCommands;
  }
}
