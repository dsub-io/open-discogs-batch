package io.dsub.discogs.batch.job.registry;

import lombok.Getter;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

public class LongIdCache {
    public LongIdCache(EntityIdRegistry.Type type) {
        this.type = type;
        this.concurrentSkipListSet = new ConcurrentSkipListSet<>();
    }

    @Getter
    private final EntityIdRegistry.Type type;
    private boolean inverted = false;
    private AtomicLong lastMax = null;
    @Getter
    private final ConcurrentSkipListSet<Long> concurrentSkipListSet;

    public boolean exists(Long item) {
        if (item == null || lastMax == null) {
            return false;
        }

        if (item > lastMax.get()) {
            return false;
        }

        if (inverted) {
            return !concurrentSkipListSet.contains(item);
        }
        return concurrentSkipListSet.contains(item);
    }

    public void add(Long item) {
        if (item == null) {
            return;
        }
        if (lastMax == null) {
            lastMax = new AtomicLong(item);
        } else if (lastMax.get() < item) {
            lastMax.set(item);
        }
        if (inverted) {
            return;
        }
        this.concurrentSkipListSet.add(item);
    }

    public boolean isInverted() {
        return this.inverted;
    }

    public void invert() {
        if (!this.inverted) {
            doInvertFromNonInverted();
        } else {
            doInvertFromInverted();
        }
        this.inverted = !inverted;
    }

    private void doInvertFromInverted() {
        if (lastMax == null || lastMax.get() < 1) {
            return;
        }
        flip();
    }

    private void doInvertFromNonInverted() {
        OptionalLong optMax = this.concurrentSkipListSet.stream()
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max();

        if (optMax.isEmpty()) {
            return;
        }

        long max = optMax.getAsLong();

        if (lastMax == null) {
            lastMax = new AtomicLong(max);
        } else if (lastMax.get() < max) {
            lastMax.set(max);
        }

        flip();
    }

    private void flip() {
        LongStream.range(1, lastMax.get() + 1).forEach(this::flipSingleValue);
    }

    private void flipSingleValue(long longValue) {
        if (this.concurrentSkipListSet.contains(longValue)) {
            this.concurrentSkipListSet.remove(longValue);
            return;
        }
        this.concurrentSkipListSet.add(longValue);
    }
}
