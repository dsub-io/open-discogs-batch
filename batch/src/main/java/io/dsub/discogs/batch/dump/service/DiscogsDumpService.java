package io.dsub.discogs.batch.dump.service;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.common.exception.DumpNotFoundException;
import java.util.Collection;
import java.util.List;

public interface DiscogsDumpService {

  void updateDB();

  // todo: implement test
  boolean exists(String eTag);

  DiscogsDump getDiscogsDump(String eTag);

  DiscogsDump getMostRecentDiscogsDumpByType(DumpType type);

  DiscogsDump getMostRecentDiscogsDumpByTypeYearMonth(DumpType type, int year, int month);

  Collection<DiscogsDump> getAllByTypeYearMonth(List<DumpType> types, int year, int month);

  List<DiscogsDump> getDumpByTypeInRange(DumpType type, int year, int month);

  List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException;

  List<DiscogsDump> getAll();
}
