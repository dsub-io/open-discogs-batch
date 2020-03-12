package io.dsub.dumpdbmgmt.batch;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.repository.ArtistRepository;
import io.dsub.dumpdbmgmt.repository.LabelRepository;
import io.dsub.dumpdbmgmt.repository.MasterReleaseRepository;
import io.dsub.dumpdbmgmt.repository.ReleaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * A component to provide writers for each entities.
 * May need major refactoring due to boilerplate codes.
 * <p>
 * todo: remove boiler plate Feb 20, 2020.
 */

@Slf4j
@Component
public class ItemWriterProvider {

    ReleaseRepository releaseRepository;
    LabelRepository labelRepository;
    ArtistRepository artistRepository;
    MasterReleaseRepository masterReleaseRepository;
    EntityManagerFactory emf;

    public ItemWriterProvider(ReleaseRepository releaseRepository,
                              LabelRepository labelRepository,
                              ArtistRepository artistRepository,
                              MasterReleaseRepository masterReleaseRepository,
                              @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
        this.artistRepository = artistRepository;
        this.masterReleaseRepository = masterReleaseRepository;
        this.emf = emf;
    }

    @Bean(value = "asyncLabelWriter")
    public AsyncItemWriter<Label> labelAsyncItemWriter(
            @Qualifier("labelRepositoryWriter") RepositoryItemWriter<Label> writer) throws Exception {
        AsyncItemWriter<Label> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer);
        try {
            asyncWriter.afterPropertiesSet();
        } catch (Exception e) {
            log.debug("failed initialize bean <asyncLabelWriter>");
            throw e;
        }
        return asyncWriter;
    }

    @Bean(value = "asyncReleaseWriter")
    public AsyncItemWriter<Release> releaseAsyncItemWriter(
            @Qualifier("releaseRepositoryWriter") RepositoryItemWriter<Release> writer) throws Exception {
        AsyncItemWriter<Release> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer);
        try {
            asyncWriter.afterPropertiesSet();
        } catch (Exception e) {
            log.debug("failed initialize bean <asyncReleaseWriter>");
            throw e;
        }
        return asyncWriter;
    }


    @Bean(value = "asyncArtistWriter")
    public AsyncItemWriter<Artist> artistAsyncItemWriter(@Qualifier("artistRepositoryWriter") RepositoryItemWriter<Artist> writer) throws Exception {
        AsyncItemWriter<Artist> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(writer);
        try {
            asyncWriter.afterPropertiesSet();
        } catch (Exception e) {
            log.debug("failed initialize bean <asyncArtistWriter>");
            throw e;
        }
        return asyncWriter;
    }

    @Bean(value = "releaseRepositoryWriter")
    public RepositoryItemWriter<Release> releaseRepositoryItemWriter() throws Exception {

        RepositoryItemWriter<Release> writer = new RepositoryItemWriter<>() {
            @Override
            public void write(List<? extends Release> items) throws Exception {
                log.info("Writing release ID " + items.get(0).getId());
                super.write(items);
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
            public void write(List<? extends Label> items) throws Exception {
                log.info("Writing label ID " + items.get(0).getId());
                super.write(items);
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
            public void write(List<? extends Artist> items) throws Exception {
                if (items.size() > 0) {
                    log.info("Writing artist ID " + items.get(0).getId());
                }
                super.write(items);
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
            public void write(List<? extends MasterRelease> items) throws Exception {
                log.info("Writing masterRelease ID " + items.get(0).getId());
                super.write(items);
            }
        };
        writer.setMethodName("save");
        writer.setRepository(masterReleaseRepository);
        writer.afterPropertiesSet();
        return writer;
    }
}
