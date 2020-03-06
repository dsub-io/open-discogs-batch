package io.dsub.dumpdbmgmt.batch;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.repository.ArtistRepository;
import io.dsub.dumpdbmgmt.repository.LabelRepository;
import io.dsub.dumpdbmgmt.repository.MasterReleaseRepository;
import io.dsub.dumpdbmgmt.repository.ReleaseRepository;
import io.dsub.dumpdbmgmt.service.ArtistCreditService;
import io.dsub.dumpdbmgmt.service.LabelReleaseService;
import io.dsub.dumpdbmgmt.service.WorkReleaseService;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.util.List;

/**
 * A component to provide writers for each entities.
 * May need major refactoring due to boilerplate codes.
 * <p>
 * todo: remove boiler plate Feb 20, 2020.
 */

@Slf4j
@Component
public class RepositoryItemWriterProvider {

    ReleaseRepository releaseRepository;
    LabelRepository labelRepository;
    ArtistRepository artistRepository;
    MasterReleaseRepository masterReleaseRepository;
    ArtistCreditService artistCreditService;
    LabelReleaseService labelReleaseService;
    WorkReleaseService workReleaseService;
    EntityManagerFactory emf;

    public RepositoryItemWriterProvider(ReleaseRepository releaseRepository,
                                        LabelRepository labelRepository,
                                        ArtistRepository artistRepository,
                                        MasterReleaseRepository masterReleaseRepository,
                                        ArtistCreditService artistCreditService,
                                        LabelReleaseService labelReleaseService,
                                        WorkReleaseService workReleaseService,
                                        @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
        this.artistRepository = artistRepository;
        this.masterReleaseRepository = masterReleaseRepository;
        this.artistCreditService = artistCreditService;
        this.labelReleaseService = labelReleaseService;
        this.workReleaseService = workReleaseService;
        this.emf = emf;
    }

    @Bean(value = "releaseRepositoryWriter")
    public RepositoryItemWriter<Release> releaseRepositoryItemWriter() throws Exception {

        RepositoryItemWriter<Release> writer = new RepositoryItemWriter<>() {
            @Override
            @Synchronized
            public void write(List<? extends Release> items) throws Exception {
                super.write(items);
                log.info("started writing from {}", items.get(0).getId().toString());
            }
        };
        writer.setMethodName("save");
        writer.setRepository(releaseRepository);
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean(value = "labelRepositoryWriter")
    public RepositoryItemWriter<Label> labelRepositoryItemWriter() throws Exception {
        RepositoryItemWriter<Label> writer = new RepositoryItemWriter<>() {
            @Override
            @Synchronized
            public void write(List<? extends Label> items) throws Exception {
                super.write(items);
                log.info("started writing from {}", items.get(0).getId().toString());
            }
        };
        writer.setMethodName("save");
        writer.setRepository(labelRepository);
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean(value = "artistRepositoryWriter")
    public RepositoryItemWriter<Artist> artistRepositoryItemWriter() throws Exception {
        RepositoryItemWriter<Artist> writer = new RepositoryItemWriter<>() {
            @Override
            @Synchronized
            public void write(List<? extends Artist> items) throws Exception {
                super.write(items);
                log.info("started writing from {}", items.get(0).getId().toString());
            }
        };
        writer.setMethodName("save");
        writer.setRepository(artistRepository);
        writer.afterPropertiesSet();
        return writer;
    }


    @Bean(value = "masterReleaseRepositoryWriter")
    public RepositoryItemWriter<MasterRelease> masterReleaseRepositoryItemWriter() throws Exception {
        RepositoryItemWriter<MasterRelease> writer = new RepositoryItemWriter<>() {
            @Override
            @Synchronized
            public void write(List<? extends MasterRelease> items) throws Exception {
                super.write(items);
                log.info("started writing from {}", items.get(0).getId().toString());
            }
        };
        writer.setMethodName("save");
        writer.setRepository(masterReleaseRepository);
        writer.afterPropertiesSet();
        return writer;
    }
}
