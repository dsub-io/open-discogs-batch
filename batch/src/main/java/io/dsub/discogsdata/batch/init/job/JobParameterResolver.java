package io.dsub.discogsdata.batch.init.job;

import java.util.Properties;
import org.springframework.boot.ApplicationArguments;

public interface JobParameterResolver {

  Properties resolve(ApplicationArguments applicationArguments);
}
