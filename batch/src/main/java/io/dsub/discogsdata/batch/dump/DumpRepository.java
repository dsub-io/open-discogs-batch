package io.dsub.discogsdata.batch.dump;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DumpRepository extends JpaRepository<DumpItem, Long> {
    boolean existsByeTag(String etag);

    DumpItem findTopByDumpTypeOrderByIdDesc(DumpType dumpType);

    List<DumpItem> findAllByDumpTypeOrderByLastModifiedDesc(DumpType dumpType);

    List<DumpItem> findAllByLastModifiedIsBetween(LocalDateTime begin, LocalDateTime end);

    DumpItem findByDumpTypeAndLastModifiedIsBetween(DumpType dumpType, LocalDateTime begin, LocalDateTime end);

    DumpItem findByeTag(String etag);

    DumpItem findTopByOrderByLastModifiedDesc();

    int countByLastModifiedBetween(LocalDateTime begin, LocalDateTime end);
}