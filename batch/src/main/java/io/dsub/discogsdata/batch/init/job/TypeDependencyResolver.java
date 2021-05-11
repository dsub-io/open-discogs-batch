package io.dsub.discogsdata.batch.init.job;

import io.dsub.discogsdata.batch.argument.ArgType;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TypeDependencyResolver {

  private static final String ARTIST = "artist";
  private static final String LABEL = "label";
  private static final String MASTER = "master";
  private static final String RELEASE = "release";

  public List<String> resolveType(ApplicationArguments args) {
    String argTypeName = ArgType.TYPE.getGlobalName();
    boolean entryExists = args.containsOption(argTypeName);

    Set<String> typesToReturn = new HashSet<>();

    if (entryExists) {
      args.getOptionValues(argTypeName)
          .forEach(type -> typesToReturn.addAll(getDependantTypes(DumpType.of(type))));
      return List.copyOf(typesToReturn);
    }

    return List.of(ARTIST, LABEL, MASTER, RELEASE);
  }

  protected List<String> getDependantTypes(DumpType dumpType) {
    switch (dumpType) {
      case ARTIST:
        return List.of(ARTIST);
      case LABEL:
        return List.of(LABEL);
      case MASTER:
        return List.of(ARTIST, LABEL, MASTER);
      case RELEASE:
        return List.of(ARTIST, LABEL, MASTER, RELEASE);
    }
    throw new InvalidArgumentException("failed to recognize following type: " + dumpType);
  }
}
