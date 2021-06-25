package io.dsub.discogs.batch.job.reader;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand;
import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.domain.master.MasterSubItemsCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand;
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
    public SynchronizedItemStreamReader<ArtistCommand> artistStreamReader() {
        try {
            return readerBuilder.build(ArtistCommand.class, artistDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize artist stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ArtistSubItemsCommand> artistSubItemsStreamReader() {
        try {
            return readerBuilder.build(ArtistSubItemsCommand.class, artistDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize artist stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<LabelCommand> labelStreamReader() {
        try {
            return readerBuilder.build(LabelCommand.class, labelDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize label stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<LabelSubItemsCommand> labelSubItemsStreamReader() {
        try {
            return readerBuilder.build(LabelSubItemsCommand.class, labelDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize label stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MasterCommand> masterStreamReader() {
        try {
            return readerBuilder.build(MasterCommand.class, masterDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize master stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MasterSubItemsCommand> masterSubItemsStreamReader() {
        try {
            return readerBuilder.build(MasterSubItemsCommand.class, masterDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize master stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ReleaseItemCommand> releaseItemStreamReader() {
        try {
            return readerBuilder.build(ReleaseItemCommand.class, releaseItemDump(null));
        } catch (Exception e) {
            throw new InitializationFailureException(
                    "failed to initialize release stream reader: " + e.getMessage());
        }
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<ReleaseItemSubItemsCommand> releaseItemSubItemsStreamReader() {
        try {
            return readerBuilder.build(ReleaseItemSubItemsCommand.class, releaseItemDump(null));
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
