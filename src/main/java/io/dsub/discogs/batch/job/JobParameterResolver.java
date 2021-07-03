package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;

import java.util.Properties;

public interface JobParameterResolver {
    Properties resolve(ApplicationArguments applicationArguments) throws InvalidArgumentException, DumpNotFoundException;
}
