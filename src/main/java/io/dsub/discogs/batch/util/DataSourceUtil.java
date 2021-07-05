package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.datasource.DataSourceDetails;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.jooq.SQLDialect;
import org.springframework.util.Assert;

/**
 * A utility class to reduce data source related repeated methods.
 */
@Slf4j
public final class DataSourceUtil {

  public static final Pattern JDBC_URL_PATTERN = Pattern
      .compile("jdbc:\\w+://[.\\w]+:[\\d]+/(\\w+).*");

  /* prevent instantiation */
  private DataSourceUtil() {
  }

  public static DataSourceDetails getDataSourceDetails(DataSource dataSource) {
    DBType type = getDBTypeFrom(dataSource);
    Assert.notNull(type, "DBType cannot be null");
    SQLDialect dialect = getSQLDialect(type);
    return new DataSourceDetails(dataSource, dialect, type);
  }

  public static SQLDialect getSQLDialect(DBType dbType) {
    return SQLDialect.POSTGRES;
  }

  public static DBType getDBTypeFrom(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return DBType.getTypeOf(conn.getMetaData().getDatabaseProductName());
    } catch (SQLException e) {
      log.error("failed to establish connection.", e);
      return null;
    }
  }

  public static DatabaseMetaData getMetaData(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return conn.getMetaData();
    } catch (SQLException e) {
      log.error("failed to establish connection.", e);
      return null;
    }
  }

  public static String getCatalogName(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return conn.getCatalog();
    } catch (SQLException e) {
      log.error("failed to establish connection.", e);
      return null;
    }
  }
}
