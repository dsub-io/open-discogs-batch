package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Slf4j
@Configuration
public class JooqConfig {

    private static final String POSTGRESQL = "PostgreSQL";
    private static final String MYSQL = "MySQL";


    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        SQLDialect dialect;

        try (Connection conn = dataSource.getConnection()) {
            dialect = getDialectByProductName(conn.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            log.error("failed to obtain connection for database", e);
            throw new InitializationFailureException("failed to obtain connection for database: " + e.getMessage());
        }
        return DSL.using(dataSource, dialect);
    }

    protected SQLDialect getDialectByProductName(String productName) {
        if (POSTGRESQL.equals(productName)) {
            return SQLDialect.POSTGRES;
        }
        return SQLDialect.MYSQL;
    }
}