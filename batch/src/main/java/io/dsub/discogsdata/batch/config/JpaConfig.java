package io.dsub.discogsdata.batch.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"io.dsub.discogsdata.common.entity", "io.dsub.discogsdata.batch.dump"})
@EnableJpaRepositories(basePackages = {"io.dsub.discogsdata.common.entity", "io.dsub.discogsdata.batch.dump"})
public class JpaConfig {

}