package io.dsub.discogs.batch.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum DBType {
    H2("org.h2.Driver"), POSTGRESQL("org.postgresql.Driver"), MYSQL("com.mysql.cj.jdbc.Driver"), MARIADB("org.mariadb.jdbc.Driver");

    @Getter
    private final String driverClassName;

    public static List<String> getNames() {
        return Arrays.stream(values())
                .map(DBType::name)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public String value() {
        return this.name().toLowerCase();
    }
}
