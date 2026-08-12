package io.dsub.discogs.batch.dump;

import java.io.File;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public interface DumpSupplier extends Supplier<List<DiscogsDump>> {

  List<DiscogsDump> get();

  List<DiscogsDump> get(File file);

  List<DiscogsDump> getLatest(Set<EntityType> entities);

  List<DiscogsDump> getMonth(Set<EntityType> entities, YearMonth month);
}
