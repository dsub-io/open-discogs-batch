package io.dsub.discogs.batch.dump;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

public interface DumpSupplier extends Supplier<List<DiscogsDump>> {

  List<DiscogsDump> get();

  List<DiscogsDump> get(File file);
}
