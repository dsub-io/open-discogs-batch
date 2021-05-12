package io.dsub.discogsdata.batch.dump.repository;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

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