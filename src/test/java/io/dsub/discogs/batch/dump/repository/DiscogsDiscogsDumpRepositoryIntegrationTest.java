package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.condition.RequiresDiscogsDataConnection;
import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(RequiresDiscogsDataConnection.class)
class DiscogsDiscogsDumpRepositoryIntegrationTest {

    static DumpSupplier dumpSupplier;
    static DiscogsDumpRepository repository;

    @BeforeAll
    static void beforeAll() throws Exception {
        dumpSupplier = new DefaultDumpSupplier();
        MapDiscogsDumpRepository mapDiscogsDumpRepository = new MapDiscogsDumpRepository(dumpSupplier);
        mapDiscogsDumpRepository.afterPropertiesSet();
        repository = mapDiscogsDumpRepository;
    }

    @Test
    void whenFindAll__ShouldNotReturnEmptyList() {
        // when
        List<DiscogsDump> found = repository.findAll();

        // then
        assertThat(found).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void whenFindTopByType__ShouldReturnDiscogsDumpWithValidValues(EntityType type) {
        // when
        DiscogsDump dump = repository.findTopByType(type);

        // then
        assertAll(
                () -> assertThat(dump.getLastModifiedAt()).isNotNull(),
                () -> assertThat(dump.getETag()).isNotNull(),
                () -> assertThat(dump.getSize()).isNotNull(),
                () -> assertThat(dump.getUriString()).isNotNull(),
                () -> assertThat(dump.getFileName()).isNotNull(),
                () -> assertThat(dump.getType()).isNotNull(),
                () -> assertThat(dump.getUrl()).isNotNull()
        );
    }
}
