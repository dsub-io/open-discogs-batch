package io.dsub.discogs.batch.job.registry;

import lombok.Getter;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class IdCache {
    public IdCache(EntityIdRegistry.Type type) {
        this.type = type;
        this.concurrentSkipListSet = new ConcurrentSkipListSet<>();
    }

    @Getter
    private final EntityIdRegistry.Type type;
    private boolean inverted = false;
    private AtomicInteger lastMax = null;
    @Getter
    private final ConcurrentSkipListSet<Integer> concurrentSkipListSet;

    public boolean exists(Integer item) {
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

    public void add(Integer item) {
        if (item == null) {
            return;
        }
        if (lastMax == null) {
            lastMax = new AtomicInteger(item);
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
        System.gc(); // force gc call for mem clear
    }

    private void doInvertFromInverted() {
        if (lastMax == null || lastMax.get() < 1) {
            return;
        }
        flip();
    }

    private void doInvertFromNonInverted() {
        OptionalInt optMax = this.concurrentSkipListSet.stream()
                .filter(Objects::nonNull)
                .mapToInt(num -> num)
                .max();

        if (optMax.isEmpty()) {
            return;
        }

        int max = optMax.getAsInt();

        if (lastMax == null) {
            lastMax = new AtomicInteger(max);
        } else if (lastMax.get() < max) {
            lastMax.set(max);
        }

        flip();
    }

    private void flip() {
        IntStream.range(1, lastMax.get() + 1).forEach(this::flipSingleValue);
    }

    private void flipSingleValue(int intValue) {
        if (this.concurrentSkipListSet.contains(intValue)) {
            this.concurrentSkipListSet.remove(intValue);
            return;
        }
        this.concurrentSkipListSet.add(intValue);
    }
}
