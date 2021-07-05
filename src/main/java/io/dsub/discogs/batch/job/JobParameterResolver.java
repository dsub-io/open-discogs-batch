package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Properties;
import org.springframework.boot.ApplicationArguments;

public interface JobParameterResolver {

  Properties resolve(ApplicationArguments applicationArguments)
      throws InvalidArgumentException, DumpNotFoundException;
}
