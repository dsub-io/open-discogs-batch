package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DumpServiceImpl implements DumpService, InitializingBean {

    private final DumpRepository dumpRepository;
    private final DumpFetcher dumpFetcher;
    private boolean initialized = false;

    @Override
    public DumpItem getDumpByEtag(String etag) throws DumpNotFoundException {
        etag = etag.replace("\"", "");
        if (!dumpRepository.existsByeTag(etag)) {
            throw new DumpNotFoundException(etag);
        }
        return dumpRepository.findByeTag(etag);
    }

    @Override
    public DumpItem getMostRecentDumpByType(DumpType ENTITY) {
        return dumpRepository.findTopByDumpTypeOrderByIdDesc(ENTITY);
    }

    @Override
    public List<DumpItem> getLatestCompletedDumpSet() {
        LocalDateTime current = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        LocalDateTime start = LocalDateTime.of(current.toLocalDate(), current.toLocalTime());

        List<DumpItem> dumps = new ArrayList<>();

        while (dumps.size() < 4) {
            dumps = dumpRepository.findAllByLastModifiedIsBetween(start, start.plusMonths(1));
            start = start.minusMonths(1);
        }

        return dumps;
    }

    @Override
    public List<DumpItem> getDumpListInRange(LocalDateTime start, LocalDateTime end) {
        return dumpRepository
                .findAllByLastModifiedIsBetween(start, end);
    }

    @Override
    public DumpItem getDumpByDumpTypeInRange(DumpType dumpType, LocalDateTime from, LocalDateTime to) {
        return dumpRepository.findByDumpTypeAndLastModifiedIsBetween(dumpType, from, to);
    }

    @Override
    public List<DumpItem> getDumpListInYearMonth(int year, int month) {
        LocalDateTime start = getYearMonthInitialDateTime(year, month);
        return dumpRepository.findAllByLastModifiedIsBetween(start, start.plusMonths(1));
    }

    @Override
    public void afterPropertiesSet() {
        updateDumps();
        initialized = true;
    }

    @Override
    public void updateDumps() {
        if (initialized) return;

        OffsetDateTime current = OffsetDateTime.now(ZoneId.of("UTC"));
        int month = current.getMonthValue();
        int year = current.getYear();

        LocalDateTime start = LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.MIN);
        LocalDateTime end = start.plusMonths(1);

        int count = dumpRepository.countByLastModifiedBetween(start, end);

        if (count == 4) {
            log.debug("full monthly dump found for {}-{}", year, month);
            return;
        }

        List<DumpItem> target = dumpFetcher.getDiscogsDumps();
        long repoSize = dumpRepository.count();

        if (repoSize == target.size()) {
            log.debug("repository up-to-date. skip update");
            return;
        }

        if (repoSize < target.size()) {
            log.debug("begin update >> current {} target {}", repoSize, target.size());
        }

        target = target.stream()
                .filter(item -> !dumpRepository.existsByeTag(item.getETag()))
                .collect(Collectors.toList());

        List<?> result = dumpRepository.saveAll(target);
        log.info("persisted {} dump records", result.size());
    }

    @Override
    public List<DumpItem> getAllDumps() {
        return dumpRepository.findAll();
    }

    @Override
    public boolean isExistsByEtag(String etag) {
        return dumpRepository.existsByeTag(etag);
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    private OffsetDateTime getCurrentYearMonthDateTime() {
        return OffsetDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withOffsetSameInstant(ZoneOffset.UTC);
    }

    private LocalDateTime getYearMonthInitialDateTime(int year, int month) {
        return LocalDateTime.now()
                .withYear(year)
                .withMonth(month)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
