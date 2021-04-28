package io.dsub.discogsdata.batch.dump;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDumpDependencyResolver implements DumpDependencyResolver {

    private final DumpService dumpService;

    @Override
    public List<DumpItem> resolveByETags(Collection<String> etag) {
        Map<DumpType, DumpItem> dumpMap = getDumpMap(etag);
        dumpMap = resolveDumpDependency(dumpMap);
        return new ArrayList<>(dumpMap.values());
    }

    @Override
    public List<DumpItem> resolveByTypes(Collection<String> types) {

        log.debug("resolving dump dependencies by given types: {}", types);

        Map<DumpType, DumpItem> dumpMap = types.stream()
                .map(type -> DumpType.valueOf(type.toUpperCase(Locale.ROOT)))
                .map(dumpService::getMostRecentDumpByType)
                .collect(Collectors.toMap(DumpItem::getDumpType, dump -> dump));

        return new ArrayList<>(resolveDumpDependency(dumpMap).values());
    }

    @Override
    public List<DumpItem> resolveByYearMonth(String yearMonth) {
        LocalDateTime targetDateTime = parseDateTimeByYearMonth(yearMonth);
        return new ArrayList<>(dumpService.getDumpListInRange(targetDateTime, targetDateTime.plusMonths(1)));
    }

    @Override
    public List<DumpItem> resolveByTypesAndYearMonth(Collection<String> types, String yearMonth) {
        LocalDateTime targetDateTime = parseDateTimeByYearMonth(yearMonth);

        List<DumpType> typeList = types.stream()
                .map(String::toUpperCase)
                .map(DumpType::valueOf)
                .collect(Collectors.toList());

        Map<DumpType, DumpItem> dumpMap = new HashMap<>();

        typeList.forEach(type -> {
            DumpItem dump = dumpService
                    .getDumpByDumpTypeInRange(type, targetDateTime, targetDateTime.plusMonths(1));
            dumpMap.put(dump.getDumpType(), dump);
        });

        return new ArrayList<>(resolveDumpDependency(dumpMap).values());
    }

    private Map<DumpType, DumpItem> getDumpMap(Collection<String> etags) {
        Map<DumpType, DumpItem> dumpMap = new HashMap<>();
        for (String etag : etags) {
            if (dumpService.isExistsByEtag(etag)) {
                DumpItem dump = dumpService.getDumpByEtag(etag);
                if (dumpMap.containsKey(dump.getDumpType())) {
                    DumpItem previous = dumpMap.get(dump.getDumpType());
                    if (previous.getLastModified().isBefore(dump.getLastModified())) {
                        dumpMap.put(dump.getDumpType(), dump);
                        continue;
                    }
                }
                dumpMap.put(dump.getDumpType(), dump);
            }
        }
        return dumpMap;
    }

    private LocalDateTime parseDateTimeByYearMonth(String yearMonth) {
        List<Integer> nums = Arrays.stream(yearMonth.split("-"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDate localDate = LocalDate.of(nums.get(0), nums.get(1), 1);
        return LocalDateTime.of(localDate, LocalTime.MIN);
    }

    private Map<DumpType, DumpItem> resolveDumpDependency(Map<DumpType, DumpItem> dumpMap) {

        Map<DumpType, DumpItem> resultMap = new HashMap<>();
        dumpMap.forEach(resultMap::put);

        if (resultMap.containsKey(DumpType.RELEASE)) {
            return getDumpTypeDiscogsDumpMap(DumpType.RELEASE, resultMap);
        }

        if (resultMap.containsKey(DumpType.MASTER)) {
            return getDumpTypeDiscogsDumpMap(DumpType.MASTER, resultMap);
        }

        log.debug("no dependencies found. proceed...");
        return resultMap;
    }

    private Map<DumpType, DumpItem> getDumpTypeDiscogsDumpMap(DumpType dumpType, Map<DumpType, DumpItem> dumpMap) {

        List<DumpType> dependantTypes = getDependantTypes(dumpType);
        DumpItem referenceDump = dumpMap.get(dumpType);

        LocalDateTime targetDateTime = referenceDump.getLastModified();
        log.debug("resolving dependencies. fetching relevant dumps from: " + targetDateTime.getMonth() + ", " + targetDateTime.getYear());
        List<DumpItem> list = dumpService.getDumpListInYearMonth(targetDateTime.getYear(), targetDateTime.getMonthValue());

        List<String> foundDumpStrings = reportDumpListAsStrings(list);
        log.debug("found following dumps from given criteria: {}", foundDumpStrings);

        list = list.stream()
                .filter(item -> dependantTypes.contains(item.getDumpType()))
                .collect(Collectors.toList());

        list.forEach(replaceOlderDumps(dumpMap));

        if (dumpType.equals(DumpType.RELEASE) && list.size() < 4) {
            pourMissingTypes(list, DumpType.values());
        }

        if (dumpType.equals(DumpType.MASTER) && list.size() < 3) {
            pourMissingTypes(list, new DumpType[]{DumpType.ARTIST, DumpType.LABEL, DumpType.MASTER});
        }

        log.debug("dumpMap: {}", dumpMap);

        // overwrite or fill the required dump into dumpMap
        list.forEach(dump -> dumpMap.put(dump.getDumpType(), dump));
        log.debug("completed resolving dump dependencies: {}", reportDumpListAsStrings(list));

        return dumpMap;
    }

    private List<String> reportDumpListAsStrings(List<DumpItem> list) {
        return list.stream()
                .map(item -> item.getDumpType() + "{" + item.getETag() + "}")
                .collect(Collectors.toList());
    }

    private void pourMissingTypes(List<DumpItem> list, DumpType[] types) {
        List<DumpType> fetchedTypes = list.stream()
                .map(DumpItem::getDumpType)
                .collect(Collectors.toList());

        List<DumpType> missingTypes = Arrays.stream(types)
                .filter(type -> !fetchedTypes.contains(type))
                .collect(Collectors.toList());

        log.debug("pouring missing types in given year and month: {}", missingTypes);
        missingTypes.forEach(type -> list.add(dumpService.getMostRecentDumpByType(type)));
    }

    private List<DumpType> getDependantTypes(DumpType type) {
        List<DumpType> types = new ArrayList<>(Arrays.asList(DumpType.LABEL, DumpType.ARTIST));
        switch (type) {
            case RELEASE: {
                types.add(DumpType.MASTER);
                return types;
            }
            case MASTER:
                return types;
            default:
                return new ArrayList<>();
        }
    }

    private Consumer<DumpItem> replaceOlderDumps(Map<DumpType, DumpItem> resultMap) {
        return item -> {
            if (resultMap.containsKey(item.getDumpType())) {
                DumpItem preListedDump = resultMap.get(item.getDumpType());
                if (item.getLastModified().isAfter(preListedDump.getLastModified())) {
                    log.debug("replacing dump {{}} to {{}}", preListedDump.getETag(), item.getETag());
                    resultMap.put(item.getDumpType(), item);
                }
            }
        };
    }
}
