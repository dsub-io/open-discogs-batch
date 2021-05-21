package io.dsub.discogsdata.batch.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
@EnableJpaRepositories(basePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
public class JpaConfig {

}