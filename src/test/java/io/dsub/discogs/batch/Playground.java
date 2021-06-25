package io.dsub.discogs.batch;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class Playground {
    @RepeatedTest(100)
    void test() {
        EntityIdRegistry registry = new EntityIdRegistry();

        Random rand = new Random();

        List<Integer> existing = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();

        int max = 0;

        for (int i = 1; i < 100; i++) {
            if (i % (rand.nextInt(5) + 1) == 0) {
                missing.add(i);
                continue;
            }
            existing.add(i);
            max = Integer.max(i, max);
            registry.put(EntityIdRegistry.Type.ARTIST, Long.valueOf(i));
        }

        int currMax = max;

        missing = missing.stream().filter(i -> i < currMax).collect(Collectors.toList());

        for (Integer i1 : missing) {
            assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, i1)).isFalse();
        }
        for (Integer integer : existing) {
            assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, integer)).isTrue();
        }

        System.out.println("CURRENT >> " + registry.getLongIdCache(EntityIdRegistry.Type.ARTIST).getConcurrentSkipListSet());
        registry.invert(EntityIdRegistry.Type.ARTIST); // now inverted data
        System.out.println("FLIPPED >> " + registry.getLongIdCache(EntityIdRegistry.Type.ARTIST).getConcurrentSkipListSet());

        for (Integer integer : missing) {
            if (registry.getLongIdCache(EntityIdRegistry.Type.ARTIST).exists((long)integer)) {
                System.out.println(missing);
                System.out.println(registry.getLongIdCache(EntityIdRegistry.Type.ARTIST).getConcurrentSkipListSet());
            }
        }
        for (Integer i : existing) {
            assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, i)).isTrue();
        }
    }
}
