package io.dsub.discogs.batch.job.registry;

import java.util.concurrent.ConcurrentSkipListSet;

public class EntityIdRegistry {

    private final LongIdCache artistCache = new LongIdCache(Type.ARTIST);
    private final LongIdCache masterCache = new LongIdCache(Type.MASTER);
    private final LongIdCache labelCache = new LongIdCache(Type.LABEL);
    private final ConcurrentSkipListSet<String> genreSet = new ConcurrentSkipListSet<>();
    private final ConcurrentSkipListSet<String> styleSet = new ConcurrentSkipListSet<>();

    public boolean exists(Type type, long id) {
        return getLongIdCache(type).exists(id);
    }

    public boolean exists(Type type, String id) {
        return getStringIdSetByType(type).contains(id);
    }

    public void put(Type type, Long id) {
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
            case ARTIST:
                artistCache.invert();
                break;
            case LABEL:
                labelCache.invert();
                break;
            case MASTER:
                masterCache.invert();
                break;
        }
    }

    public ConcurrentSkipListSet<String> getStringIdSetByType(Type type) {
        if (type.equals(Type.GENRE)) {
            return genreSet;
        }
        return styleSet;
    }

    public LongIdCache getLongIdCache(Type type) {
        switch (type) {
            case ARTIST:
                return artistCache;
            case LABEL:
                return labelCache;
            default:
                return masterCache;
        }
    }

    public enum Type {
        ARTIST, LABEL, MASTER, RELEASE, GENRE, STYLE
    }
}