package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@EnableJpaAuditing
@EnableBatchProcessing
@EntityScan(basePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
@EnableJpaRepositories(basePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
@SpringBootApplication(scanBasePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
public class BatchApplication {
    public static void main(String[] args) {
        try {
            args = new DefaultArgumentHandler().resolve(args);
        } catch (InvalidArgumentException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        SpringApplication.run(BatchApplication.class, args);
    }
}