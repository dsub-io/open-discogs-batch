package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.artist.entity.Artist;
import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class ArtistCoreProcessor implements ItemProcessor<ArtistCommand, BaseEntity> {

    @Override
    public Artist process(ArtistCommand command) {
        if (command.getId() == null || command.getId() < 1) {
            return null;
        }

        ReflectionUtil.normalizeStringFields(command);

        return Artist.builder()
                .id(command.getId())
                .name(command.getName())
                .realName(command.getRealName())
                .profile(command.getProfile())
                .dataQuality(command.getDataQuality())
                .build();
    }
}