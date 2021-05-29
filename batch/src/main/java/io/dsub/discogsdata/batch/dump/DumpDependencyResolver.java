package io.dsub.discogsdata.batch.dump;

import java.util.Collection;
import org.springframework.boot.ApplicationArguments;

public interface DumpDependencyResolver {

  Collection<DiscogsDump> resolve(ApplicationArguments args);
}
