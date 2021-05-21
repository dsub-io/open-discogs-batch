package io.dsub.discogsdata.batch.init.job;

import static io.dsub.discogsdata.batch.config.BatchConfig.DEFAULT_CHUNK_SIZE;
import static io.dsub.discogsdata.batch.config.BatchConfig.DEFAULT_THROTTLE_LIMIT;

import io.dsub.discogsdata.batch.argument.ArgType;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
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
  private static final String THROTTLE_LIMIT = ArgType.THROTTLE_LIMIT.getGlobalName();
  private final DumpDependencyResolver dumpDependencyResolver;

  @Override
  public Properties resolve(ApplicationArguments args) {
    Properties props = new Properties();
    dumpDependencyResolver
        .resolve(args)
        .forEach(dump -> props.put(dump.getType().toString(), dump.getETag())); // add all dumps
    props.put(THROTTLE_LIMIT, String.valueOf(parseThrottleLimit(args)));
    props.put(CHUNK_SIZE, String.valueOf(parseChunkSize(args)));
    return props;
  }

  protected int parseThrottleLimit(ApplicationArguments args) {
    return parse(args, ArgType.THROTTLE_LIMIT, DEFAULT_THROTTLE_LIMIT);
  }

  protected int parseChunkSize(ApplicationArguments args) {
    return parse(args, ArgType.CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
  }

  private int parse(ApplicationArguments args, ArgType argType, int defaultValue) {
    String optionName = argType.getGlobalName();
    if (args.containsOption(optionName)) {
      String toParse = args.getOptionValues(optionName).get(0);
      try {
        return Integer.parseInt(toParse);
      } catch (NumberFormatException ignored) {
        throw new InvalidArgumentException("failed to parse " + optionName + ": " + toParse);
      }
    }
    log.debug(optionName + " not specified. returning default value: " + defaultValue);
    return defaultValue;
  }
}
