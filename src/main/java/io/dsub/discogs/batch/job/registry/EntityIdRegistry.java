package io.dsub.discogs.batch.job.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class EntityIdRegistry {

    private final IdCache artistCache = new IdCache(Type.ARTIST);
    private final IdCache masterCache = new IdCache(Type.MASTER);
    private final IdCache labelCache = new IdCache(Type.LABEL);
    private final IdCache releaseItemCache = new IdCache(Type.LABEL);
    private final ConcurrentSkipListSet<String> genreSet = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<String> styleSet = new ConcurrentSkipListSet<>();

    public boolean exists(Type type, Integer id) {
        if (id == null || id < 1) {
            return false;
        }
        return getLongIdCache(type).exists(id);
    }

    public boolean exists(Type type, String id) {
        return getStringIdSetByType(type).contains(id);
    }

    public void put(Type type, Integer id) {
        if (type != null && id != null) {
            getLongIdCache(type).add(id);
        }
    }

    public void put(Type type, String id) {
        if (id != null && !id.isBlank()) {
            getStringIdSetByType(type).add(id);
        }
    }

    public void invert(Type type) {
        switch (type) {
            case ARTIST -> artistCache.invert();
            case LABEL -> labelCache.invert();
            case MASTER -> masterCache.invert();
        }
    }

    public void clearAll() {
        for (Type t : List.of(Type.ARTIST, Type.LABEL, Type.MASTER, Type.RELEASE)) {
            getLongIdCache(t).getConcurrentSkipListSet().clear();
        }
        genreSet.clear();
        styleSet.clear();
    }

    public ConcurrentSkipListSet<String> getStringIdSetByType(Type type) {
        if (type.equals(Type.GENRE)) {
            return genreSet;
        }
        return styleSet;
    }

    public IdCache getLongIdCache(Type type) {
        return switch (type) {
            case ARTIST -> artistCache;
            case LABEL -> labelCache;
            case MASTER -> masterCache;
            default -> releaseItemCache;
        };
    }

    public enum Type {
        ARTIST, LABEL, MASTER, RELEASE, GENRE, STYLE
    }
}