package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.jooq.postgres.tables.records.ArtistRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ArtistCoreProcessor implements ItemProcessor<ArtistXML, ArtistRecord> {
    @Override
    public ArtistRecord process(ArtistXML item) {
        if (item.getId() == null || item.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(item);
        return item.buildRecord();
    }
}