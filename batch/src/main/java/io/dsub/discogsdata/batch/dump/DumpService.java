package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.exception.DumpNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

public interface DumpService {
    void updateDumps();

    DumpItem getDumpByEtag(String etag) throws DumpNotFoundException;

    DumpItem getMostRecentDumpByType(DumpType type);

    List<DumpItem> getDumpListInRange(LocalDateTime from, LocalDateTime to);

    DumpItem getDumpByDumpTypeInRange(DumpType dumpType, LocalDateTime from, LocalDateTime to);

    List<DumpItem> getDumpListInYearMonth(int year, int month);

    List<DumpItem> getLatestCompletedDumpSet();

    List<DumpItem> getAllDumps();

    boolean isExistsByEtag(String etag);

    boolean isInitialized();
}
