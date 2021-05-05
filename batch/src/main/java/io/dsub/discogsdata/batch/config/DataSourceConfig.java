package io.dsub.discogsdata.batch.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final ApplicationArguments applicationArguments;

    @Bean(name = "batchDataSource")
    public DataSource batchDataSource() {
        Map<String, String> arguments = applicationArguments.getNonOptionArgs().stream()
                .map(s -> s.split("="))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

        return DataSourceBuilder.create()
                .url(arguments.get("url"))
                .username(arguments.get("username"))
                .password(arguments.get("password"))
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}