package io.dsub.discogsdata.batch.jdbc;

import io.dsub.discogsdata.batch.dto.DtoQueryBuilder;
import io.dsub.discogsdata.batch.dto.DtoRegistry;
import io.dsub.discogsdata.batch.entity.artist.dto.ArtistAliasDTO;
import io.dsub.discogsdata.batch.entity.artist.dto.ArtistDTO;
import io.dsub.discogsdata.batch.dto.DefaultDtoQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DefaultDtoQueryBuilderTest {

    private DtoQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new DefaultDtoQueryBuilder(new DtoRegistry());
    }

    @Test
    void buildInsertQuery() {
        String s = queryBuilder.buildInsertQuery(ArtistDTO.class, false);
        assertThat(s)
                .isNotBlank()
                .contains("INSERT INTO");
        s = queryBuilder.buildInsertQuery(ArtistDTO.class, true);
        assertThat(s)
                .isNotBlank()
                .contains("INSERT INTO")
                .contains("ON DUPLICATE KEY UPDATE");
        s = queryBuilder.buildInsertQuery(ArtistAliasDTO.class, false);
        assertThat(s)
                .isNotBlank()
                .contains("INSERT INTO")
                .contains("_tmp")
                .doesNotContain("ON DUPLICATE KEY UPDATE");
        System.out.println(s);
    }

    @Test
    void buildPruneQuery() {
        String s = queryBuilder.buildComparePruneCloneTableQuery(ArtistAliasDTO.class);
        System.out.println(s);
    }


}