package io.dsub.discogsdata.batch.init.job;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import org.springframework.boot.ApplicationArguments;

import java.util.Collection;

public interface DumpDependencyResolver {
    Collection<DiscogsDump> resolve(ApplicationArguments args);
}
