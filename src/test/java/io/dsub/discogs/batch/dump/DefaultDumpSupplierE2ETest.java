package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.condition.RequiresDiscogsDataConnection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("e2e")
@ExtendWith(RequiresDiscogsDataConnection.class)
class DefaultDumpSupplierE2ETest {

  DefaultDumpSupplier dumpSupplier;

  @BeforeEach
  void setUp() {
    dumpSupplier = new DefaultDumpSupplier();
  }

  @Test
  void whenGet__ThenReturnsNotEmptyListOfValidDiscogsDumps() {
    List<DiscogsDump> foundList = dumpSupplier.get();

    assertThat(foundList).isNotNull().isNotEmpty();
    foundList.forEach(
        item ->
            assertThat(item)
                .satisfies(dump -> assertThat(dump.getETag()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getSize()).isNotNull().isGreaterThan(0))
                .satisfies(dump -> assertThat(dump.getUriString()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getFileName()).matches("^[\\w_]+.xml.gz$"))
                .satisfies(dump -> assertThat(dump.getType()).isNotNull()));
  }

  @Test
  void whenGetBucketURL__ReturnsValidURL() {
    String url = dumpSupplier.getBucketURL();

    assertThat(url).isNotNull().isNotBlank().matches("^https://[\\w_.-]+[\\w_-].com$");
  }
}
