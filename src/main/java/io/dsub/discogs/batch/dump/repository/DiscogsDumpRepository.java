package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiscogsDumpRepository extends JpaRepository<DiscogsDump, String> {
    boolean existsByeTag(String eTag);

    DiscogsDump findByeTag(String eTag);

    DiscogsDump findTopByTypeOrderByCreatedAtDesc(EntityType type);

    List<DiscogsDump> findAllByCreatedAtIsBetween(LocalDate start, LocalDate end);

    List<DiscogsDump> findByTypeAndCreatedAtBetween(EntityType type, LocalDate start, LocalDate end);

    DiscogsDump findTopByTypeAndCreatedAtBetween(EntityType type, LocalDate start, LocalDate end);

    int countAllByCreatedAtIsBetween(LocalDate start, LocalDate end);

    int countAllByCreatedAtIsGreaterThanEqual(LocalDate startDate);
}
