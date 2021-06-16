package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.config.BatchConfig;
import io.dsub.discogs.batch.dump.DumpDependencyResolver;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultJobParameterResolver implements JobParameterResolver {

  private static final String CHUNK_SIZE = ArgType.CHUNK_SIZE.getGlobalName();

  private final DumpDependencyResolver dumpDependencyResolver;

  @Override
  public Properties resolve(ApplicationArguments args)
      throws InvalidArgumentException, DumpNotFoundException {
    Properties props = new Properties();
    dumpDependencyResolver
        .resolve(args)
        .forEach(dump -> props.put(dump.getType().toString(), dump.getETag())); // add all dumps
    props.put(CHUNK_SIZE, String.valueOf(parseChunkSize(args)));
    return props;
  }

  protected int parseChunkSize(ApplicationArguments args) throws InvalidArgumentException {
    String chunkSizeOptName = ArgType.CHUNK_SIZE.getGlobalName();
    if (args.containsOption(chunkSizeOptName)) {
      String toParse = args.getOptionValues(chunkSizeOptName).get(0);
      try {
        log.debug("found entry for " + chunkSizeOptName + ": " + toParse);
        return Integer.parseInt(toParse);
      } catch (NumberFormatException ignored) {
        throw new InvalidArgumentException("failed to parse " + chunkSizeOptName + ": " + toParse);
      }
    }
    log.debug(
        chunkSizeOptName
            + " not specified. returning default value: "
            + BatchConfig.DEFAULT_CHUNK_SIZE);
    return BatchConfig.DEFAULT_CHUNK_SIZE;
  }
}
