package io.dsub.discogs.batch.dump.service;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.DumpNotFoundException;

import java.util.Collection;
import java.util.List;

public interface DiscogsDumpService {

    void updateDB();

    boolean exists(String eTag);

    DiscogsDump getDiscogsDump(String eTag) throws DumpNotFoundException;

    DiscogsDump getMostRecentDiscogsDumpByType(EntityType type);

    DiscogsDump getMostRecentDiscogsDumpByTypeYearMonth(EntityType type, int year, int month);

    Collection<DiscogsDump> getAllByTypeYearMonth(List<EntityType> types, int year, int month) throws DumpNotFoundException;

    List<DiscogsDump> getDumpByTypeInRange(EntityType type, int year, int month);

    List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException;

    List<DiscogsDump> getAll();
}
