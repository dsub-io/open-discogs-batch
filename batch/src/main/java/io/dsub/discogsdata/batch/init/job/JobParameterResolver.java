package io.dsub.discogsdata.batch.init.job;

import org.springframework.boot.ApplicationArguments;

import java.util.Properties;

public interface JobParameterResolver {
    Properties resolve(ApplicationArguments applicationArguments);
}
