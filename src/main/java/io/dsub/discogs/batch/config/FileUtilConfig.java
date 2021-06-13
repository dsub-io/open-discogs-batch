package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileUtilConfig {

  @Bean
  public FileUtil fileUtil(ApplicationArguments args) {
    boolean keepFile = args.containsOption(ArgType.MOUNT.getGlobalName());
    FileUtil fileUtil = SimpleFileUtil.builder().isTemporary(!keepFile).build();
    if (keepFile) {
      log.info("detected mount option. keeping file...");
    } else {
      log.info("mount option not set. files will be removed after the job.");
    }
    return fileUtil;
  }
}
