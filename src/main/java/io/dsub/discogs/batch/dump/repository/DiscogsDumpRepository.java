package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;

public interface DiscogsDumpRepository extends InitializingBean {

  List<DiscogsDump> findAllByLastModifiedAtIsBetween(LocalDate start, LocalDate end);

  List<DiscogsDump> findAll();

  int countItemsAfter(LocalDate start);

  int countItemsBefore(LocalDate end);

  int countItemsBetween(LocalDate start, LocalDate end);

  List<DiscogsDump> findByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end);

  DiscogsDump findTopByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end);

  DiscogsDump findTopByType(EntityType type);

  DiscogsDump findByETag(String ETag);

  boolean existsByETag(String ETag);

  int count();

  void saveAll(Collection<DiscogsDump> discogsDumps);

  void deleteAll();

  void save(DiscogsDump dump);
}
