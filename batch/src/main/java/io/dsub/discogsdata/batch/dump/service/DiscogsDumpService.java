package io.dsub.discogsdata.batch.dump.service;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.common.exception.DumpNotFoundException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface DiscogsDumpService {
  void updateDB();

  DiscogsDump getDiscogsDump(@NotNull @NotBlank @Valid String eTag);

  DiscogsDump getMostRecentDiscogsDumpByType(DumpType type);

  List<DiscogsDump> getDumpByTypeInRange(DumpType type, int year, int month);

  List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException;

  List<DiscogsDump> getAll();
}
