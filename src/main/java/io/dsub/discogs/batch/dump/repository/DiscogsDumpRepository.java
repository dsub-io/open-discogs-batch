package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscogsDumpRepository extends JpaRepository<DiscogsDump, String> {

  boolean existsByeTag(String eTag);

  DiscogsDump findByeTag(String eTag);

  DiscogsDump findTopByTypeOrderByCreatedAtDesc(DumpType type);

  List<DiscogsDump> findAllByCreatedAtIsBetween(LocalDate start, LocalDate end);

  List<DiscogsDump> findByTypeAndCreatedAtBetween(DumpType type, LocalDate start, LocalDate end);

  DiscogsDump findTopByTypeAndCreatedAtBetween(DumpType type, LocalDate start, LocalDate end);

  int countAllByCreatedAtIsBetween(LocalDate start, LocalDate end);

  int countAllByCreatedAtIsGreaterThanEqual(LocalDate startDate);
}
