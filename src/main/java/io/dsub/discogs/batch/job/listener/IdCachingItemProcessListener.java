package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemProcessListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.*;

@RequiredArgsConstructor
public class IdCachingItemProcessListener implements ItemProcessListener<Object, Object> {

    private final EntityIdRegistry idRegistry;

    /* No Op */
    @Override
    public void beforeProcess(Object item) {

    }

    @Override
    public void afterProcess(Object pulled, Object result) {
        if (result == null) {
            return;
        }
        if (pulled instanceof ArtistCommand) {
            cacheArtistId((ArtistCommand) pulled);
        } else if (pulled instanceof LabelCommand) {
            cacheArtistId((LabelCommand) pulled);
        } else if (pulled instanceof MasterCommand) {
            cacheMasterId((MasterCommand) pulled);
        }
    }


    private void cacheArtistId(ArtistCommand artist) {
        if (artist != null && artist.getId() != null) {
            idRegistry.put(ARTIST, artist.getId());
        }
    }

    private void cacheArtistId(LabelCommand label) {
        if (label != null && label.getId() != null) {
            idRegistry.put(LABEL, label.getId());
        }
    }

    private void cacheMasterId(MasterCommand master) {
        if (master != null && master.getId() != null) {
            idRegistry.put(MASTER, master.getId());
            cacheStringTypedItems(STYLE, master.getStyles());
            cacheStringTypedItems(GENRE, master.getGenres());
        }
    }

    private void cacheStringTypedItems(EntityIdRegistry.Type type, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(val -> !val.isBlank())
                .forEach(value -> idRegistry.put(type, value));
    }

    /* No Op */
    @Override
    public void onProcessError(Object item, Exception e) {

    }
}
