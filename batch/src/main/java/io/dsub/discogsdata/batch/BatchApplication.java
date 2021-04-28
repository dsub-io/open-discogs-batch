package io.dsub.discogsdata.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@ComponentScan(basePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
@EnableJpaRepositories(basePackages = "io.dsub.discogsdata")
@EntityScan(basePackages = "io.dsub.discogsdata")
@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}