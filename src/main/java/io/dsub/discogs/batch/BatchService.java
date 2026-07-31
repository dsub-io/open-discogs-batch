package io.dsub.discogs.batch;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.handler.DefaultArgumentHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class BatchService {

  private static final String USAGE =
      """
      Usage: open-discogs-batch [options]

        --database-url <uri>       PostgreSQL URI including credentials (required)
        -e, --entities <list>      artist,label,master,release (default: all)
        -m, --dump-month <yyyy-MM> Import an exact dump month (default: latest per entity)
            --data-dir <path>      Download directory (default: ~/.cache/open-discogs-batch)
        -b, --chunk-size <number>  Import chunk size (default: 5000)
        -c, --cleanup              Delete downloads after a successful import
        -f, --force                Reprocess an already successful dump
            --allow-downgrade      Allow an older dump than the entity checkpoint
        -h, --help                 Show this help
        -v, --version              Show the version

      Options can also be set with OPEN_DISCOGS_BATCH_* environment variables.
      Command-line options take precedence over environment variables.
      """;

  protected ConfigurableApplicationContext run(String[] args) throws Exception {
    if (hasOption(args, "help", "h")) {
      System.out.print(USAGE);
      return null;
    }
    if (hasOption(args, "version", "v")) {
      System.out.println("open-discogs-batch " + getVersion());
      return null;
    }
    String[] resolved = resolveArguments(args);
    return runSpringApplication(resolved);
  }

  protected ConfigurableApplicationContext runSpringApplication(String[] args) {
    return SpringApplication.run(BatchApplication.class, args);
  }

  protected String[] resolveArguments(String[] args) {
    return getArgumentHandler().resolve(args);
  }

  protected ArgumentHandler getArgumentHandler() {
    return new DefaultArgumentHandler();
  }

  private boolean hasOption(String[] args, String longName, String shortName) {
    for (String argument : args) {
      if (argument.equals("--" + longName) || argument.equals("-" + shortName)) {
        return true;
      }
    }
    return false;
  }

  private String getVersion() {
    String version = BatchService.class.getPackage().getImplementationVersion();
    return version == null || version.isBlank() ? "development" : version;
  }
}
