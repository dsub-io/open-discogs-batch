package io.dsub.discogs.batch.dump;

import java.util.Collection;
import org.springframework.boot.ApplicationArguments;

public interface DumpDependencyResolver {

  Collection<DiscogsDump> resolve(ApplicationArguments args);
}
