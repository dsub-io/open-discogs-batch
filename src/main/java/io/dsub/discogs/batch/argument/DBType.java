package io.dsub.discogs.batch.argument;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@RequiredArgsConstructor
public enum DBType {
    PostgreSQL("org.postgresql.Driver"), MySQL("com.mysql.cj.jdbc.Driver"), MariaDB("org.mariadb.jdbc.Driver");

    @Getter
    private final String driverClassName;

    public static List<String> getNames() {
        return Arrays.stream(values())
                .map(DBType::name)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public static DBType getTypeOf(String from) {
        String target =  from.toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.value().equals(target))
                .findFirst()
                .orElse(null);
    }

    public String value() {
        return this.name().toLowerCase();
    }
}
