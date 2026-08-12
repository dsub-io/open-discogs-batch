package io.dsub.discogs.batch.job.reader;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class ItemReaderConfigUnitTest {

  @Test
  void everyReaderReportsBuilderInitializationFailuresWithItsEntityContext()
      throws Exception {
    DiscogsDumpItemReaderBuilder readerBuilder = mock(DiscogsDumpItemReaderBuilder.class);
    DiscogsDumpService dumpService = mock(DiscogsDumpService.class);
    DiscogsDump dump = mock(DiscogsDump.class);
    when(dumpService.getDiscogsDump(null)).thenReturn(dump);
    doThrow(new Exception("fixture reader failure"))
        .when(readerBuilder)
        .build(any(), eq(dump));
    ItemReaderConfig config =
        new ItemReaderConfig(readerBuilder, dumpService, new EnumMap<>(EntityType.class));
    List<Callable<Object>> readers =
        List.of(
            config::artistStreamReader,
            () -> config.artistSubItemsStreamReader(5),
            config::labelStreamReader,
            () -> config.labelSubItemsStreamReader(5),
            config::masterStreamReader,
            () -> config.masterSubItemsStreamReader(5),
            () -> config.releaseItemSubItemsStreamReader(5));

    for (Callable<Object> reader : readers) {
      assertThatThrownBy(reader::call)
          .isInstanceOf(InitializationFailureException.class)
          .hasMessageContaining("fixture reader failure");
    }
  }
}
