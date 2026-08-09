package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.argument.PositiveIntegerParser;
import io.dsub.discogs.batch.config.BatchConfig;
import io.dsub.discogs.batch.dump.DumpDependencyResolver;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.opendiscogs.model.manifest.ImportManifest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
  private static final String FORCE = ArgType.FORCE.getGlobalName();
  private static final String ALLOW_DOWNGRADE = ArgType.ALLOW_DOWNGRADE.getGlobalName();

  private final DumpDependencyResolver dumpDependencyResolver;
  private final DiscogsDumpVerifier dumpVerifier;

  @Override
  public Properties resolve(ApplicationArguments args)
      throws InvalidArgumentException, DumpNotFoundException, FileException {
    Properties props = new Properties();
    Collection<DiscogsDump> dumps = dumpDependencyResolver.resolve(args);
    List<ImportManifest.Dump> manifestDumps = new ArrayList<>(dumps.size());
    for (DiscogsDump dump : dumps) {
      String checksum = dumpVerifier.getExpectedChecksum(dump);
      manifestDumps.add(
          new ImportManifest.Dump(
              dump.getType().toString(), dump.getLastModifiedAt(), checksum));
      props.put(dump.getType().toString(), dump.getETag());
      props.put(ImportJobParameters.checksum(dump.getType()), checksum);
      props.put(ImportJobParameters.date(dump.getType()), dump.getLastModifiedAt().toString());
      props.put(ImportJobParameters.etag(dump.getType()), dump.getETag());
      props.put(
          ImportJobParameters.size(dump.getType()),
          String.valueOf(dump.getSize() == null ? 0 : dump.getSize()));
      props.put(ImportJobParameters.uri(dump.getType()), dump.getUriString());
    }
    props.put(ImportJobParameters.MANIFEST_SHA256, ImportManifest.fingerprint(manifestDumps));
    props.put(CHUNK_SIZE, String.valueOf(parseChunkSize(args)));
    props.put(ImportJobParameters.FORCE, String.valueOf(args.containsOption(FORCE)));
    props.put(
        ImportJobParameters.ALLOW_DOWNGRADE,
        String.valueOf(args.containsOption(ALLOW_DOWNGRADE)));

    return props;
  }

  protected int parseChunkSize(ApplicationArguments args) throws InvalidArgumentException {
    String chunkSizeOptName = ArgType.CHUNK_SIZE.getGlobalName();
    if (args.containsOption(chunkSizeOptName)) {
      List<String> values = args.getOptionValues(chunkSizeOptName);
      String toParse = values == null || values.isEmpty() ? null : values.getFirst();
      log.debug("found entry for {}: {}", chunkSizeOptName, toParse);
      return PositiveIntegerParser.require(chunkSizeOptName, toParse);
    }
    log.debug(
        "{} not specified. returning default value: {}",
        chunkSizeOptName,
        BatchConfig.DEFAULT_CHUNK_SIZE);
    return BatchConfig.DEFAULT_CHUNK_SIZE;
  }
}
