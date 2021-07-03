package io.dsub.discogs.batch.job.reader;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
// TODO: test!
public class ItemReaderConfig {

    private static final String ARTIST_ETAG = "#{jobParameters['artist']}";
    private static final String LABEL_ETAG = "#{jobParameters['label']}";
    private static final String MASTER_ETAG = "#{jobParameters['master']}";
    private static final String RELEASE_ITEM_ETAG = "#{jobParameters['release']}";

    private final DiscogsDumpItemReaderBuilder readerBuilder;
    private final DiscogsDumpService dumpService;
    private final Map<EntityType, DiscogsDump> dumpMap;

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ArtistXML> artistStreamReader() {
        try {
            return readerBuilder.build(ArtistXML.class, artistDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize artist stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ArtistSubItemsXML> artistSubItemsStreamReader() {
        try {
            return readerBuilder.build(ArtistSubItemsXML.class, artistDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize artist stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<LabelXML> labelStreamReader() {
        try {
            return readerBuilder.build(LabelXML.class, labelDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize label stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<LabelSubItemsXML> labelSubItemsStreamReader() {
        try {
            return readerBuilder.build(LabelSubItemsXML.class, labelDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize label stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MasterXML> masterStreamReader() {
        try {
            return readerBuilder.build(MasterXML.class, masterDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize master stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MasterMainReleaseXML> masterMainReleaseStreamReader() {
        try {
            return readerBuilder.build(MasterMainReleaseXML.class, masterDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize master main release stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MasterSubItemsXML> masterSubItemsStreamReader() {
        try {
            return readerBuilder.build(MasterSubItemsXML.class, masterDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize master stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ReleaseItemXML> releaseItemStreamReader() {
        try {
            return readerBuilder.build(ReleaseItemXML.class, releaseItemDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize release stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ReleaseItemSubItemsXML> releaseItemSubItemsStreamReader() {
        try {
            return readerBuilder.build(ReleaseItemSubItemsXML.class, releaseItemDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize release stream reader: " + e.getMessage());
        }
    }

    @Bean
    @JobScope
    public DiscogsDump artistDump(@Value(ARTIST_ETAG) String eTag) throws DumpNotFoundException {
        DiscogsDump dump = dumpService.getDiscogsDump(eTag);
        dumpMap.put(EntityType.ARTIST, dump);
        return dump;
    }

    @Bean
    @JobScope
    public DiscogsDump labelDump(@Value(LABEL_ETAG) String eTag) throws DumpNotFoundException {
        DiscogsDump dump = dumpService.getDiscogsDump(eTag);
        dumpMap.put(EntityType.LABEL, dump);
        return dump;
    }

    @Bean
    @JobScope
    public DiscogsDump masterDump(@Value(MASTER_ETAG) String eTag) throws DumpNotFoundException {
        DiscogsDump dump = dumpService.getDiscogsDump(eTag);
        dumpMap.put(EntityType.MASTER, dump);
        return dump;
    }

    @Bean
    @JobScope
    public DiscogsDump releaseItemDump(@Value(RELEASE_ITEM_ETAG) String eTag) throws DumpNotFoundException {
        DiscogsDump dump = dumpService.getDiscogsDump(eTag);
        dumpMap.put(EntityType.RELEASE, dump);
        return dump;
    }
}
