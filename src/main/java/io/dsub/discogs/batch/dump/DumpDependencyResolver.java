package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;

import java.util.Collection;

public interface DumpDependencyResolver {

    Collection<DiscogsDump> resolve(ApplicationArguments args)
            throws DumpNotFoundException, InvalidArgumentException;
}
