package io.dsub.discogs.batch.datasource;

import javax.sql.DataSource;
import org.jooq.SQLDialect;

/**
 * An immutable wrapper for datasource details.
 */
public record DataSourceDetails(DataSource dataSource, SQLDialect dialect, DBType type) {

}