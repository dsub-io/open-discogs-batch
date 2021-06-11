package io.dsub.discogs.batch.job;

import java.util.Properties;
import org.springframework.boot.ApplicationArguments;

public interface JobParameterResolver {

  Properties resolve(ApplicationArguments applicationArguments);
}
