package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemProcessListener;

import java.util.List;
import java.util.Objects;

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
        if (pulled instanceof ArtistXML artist) {
            if (artist.getId() != null) {
                idRegistry.put(ARTIST, artist.getId());
            }
        } else if (pulled instanceof LabelXML label) {
            if (label.getId() != null) {
                idRegistry.put(LABEL, label.getId());
            }
        } else if (pulled instanceof MasterXML master) {
            if (master.getId() != null) {
                idRegistry.put(MASTER, master.getId());
                cacheStringTypedItems(STYLE, master.getStyles());
                cacheStringTypedItems(GENRE, master.getGenres());
            }
        } else if (pulled instanceof ReleaseItemXML releaseItem) {
            if (releaseItem.getId() != null) {
                idRegistry.put(RELEASE, releaseItem.getId());
                cacheStringTypedItems(GENRE, releaseItem.getGenres());
                cacheStringTypedItems(STYLE, releaseItem.getStyles());
            }
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
