package io.dsub.discogsdata.batch.dump;

import org.w3c.dom.NodeList;

import java.util.List;

public interface DumpFetcher {
    List<DumpItem> getDiscogsDumps();

    DumpItem parseDump(NodeList dataNodeList);
}
