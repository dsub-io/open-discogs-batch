package io.dsub.discogsdata.batch.dump;

import java.util.Collection;
import java.util.List;

public interface DumpDependencyResolver {
    List<DumpItem> resolveByETags(Collection<String> etag);

    List<DumpItem> resolveByTypes(Collection<String> types);

    List<DumpItem> resolveByYearMonth(String yearMonth);

    List<DumpItem> resolveByTypesAndYearMonth(Collection<String> types, String yearMonth);
}