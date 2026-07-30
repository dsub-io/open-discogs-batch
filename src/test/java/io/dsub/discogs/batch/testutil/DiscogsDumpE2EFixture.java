package io.dsub.discogs.batch.testutil;

import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import java.util.List;

public final class DiscogsDumpE2EFixture {

  private static List<DiscogsDump> dumps;

  private DiscogsDumpE2EFixture() {
  }

  public static synchronized List<DiscogsDump> getDumps() {
    if (dumps == null) {
      dumps = new DefaultDumpSupplier().get();
    }
    return dumps;
  }
}
