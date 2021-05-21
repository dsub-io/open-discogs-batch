package io.dsub.discogsdata.batch.init.job;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import java.util.Collection;
import org.springframework.boot.ApplicationArguments;

public interface DumpDependencyResolver {

  Collection<DiscogsDump> resolve(ApplicationArguments args);
}
