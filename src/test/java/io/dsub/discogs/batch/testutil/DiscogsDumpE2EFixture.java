package io.dsub.discogs.batch.testutil;

import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import java.io.IOException;
import java.util.List;

public final class DiscogsDumpE2EFixture {

  private static List<DiscogsDump> dumps;

  private DiscogsDumpE2EFixture() {
  }

  public static synchronized List<DiscogsDump> getDumps() {
    if (dumps == null) {
      List<DiscogsDump> fetchedDumps = new LatestYearDumpSupplier().getLatestYearDumps();
      if (fetchedDumps.isEmpty()) {
        throw new IllegalStateException(
            "Discogs latest-year index returned no usable dumps");
      }
      dumps = List.copyOf(fetchedDumps);
    }
    return dumps;
  }

  private static final class LatestYearDumpSupplier extends DefaultDumpSupplier {

    private List<DiscogsDump> getLatestYearDumps() {
      try {
        List<String> yearIndexUrls =
            parseYearIndexUrls(getDiscogsDataSource("https://data.discogs.com/"));
        String latestYearIndexUrl =
            yearIndexUrls.stream()
                .max(String::compareTo)
                .orElseThrow(
                    () -> new IllegalStateException("Discogs root index contains no year links"));
        return parseHtmlDumpList(getDiscogsDataSource(latestYearIndexUrl));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("interrupted while fetching the Discogs live index", e);
      } catch (IOException e) {
        throw new IllegalStateException("failed to fetch the Discogs live index", e);
      }
    }
  }
}
