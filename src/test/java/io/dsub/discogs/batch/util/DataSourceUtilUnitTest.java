package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.datasource.DataSourceDetails;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

class DataSourceUtilUnitTest {

  @Test
  void shouldReadPostgreSqlMetadataAndCatalog() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenReturn(metadata);
    when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
    when(connection.getCatalog()).thenReturn("discogs");

    DataSourceDetails details = DataSourceUtil.getDataSourceDetails(dataSource);

    assertThat(details.dataSource()).isSameAs(dataSource);
    assertThat(details.type()).isEqualTo(DBType.POSTGRESQL);
    assertThat(details.dialect()).isEqualTo(SQLDialect.POSTGRES);
    assertThat(DataSourceUtil.getMetaData(dataSource)).isSameAs(metadata);
    assertThat(DataSourceUtil.getCatalogName(dataSource)).isEqualTo("discogs");
    assertThat(DataSourceUtil.getSQLDialect(DBType.POSTGRESQL)).isEqualTo(SQLDialect.POSTGRES);
  }

  @Test
  void shouldReturnNullWhenDatabaseConnectionCannotBeOpened() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException("unavailable"));

    assertThat(DataSourceUtil.getDBTypeFrom(dataSource)).isNull();
    assertThat(DataSourceUtil.getMetaData(dataSource)).isNull();
    assertThat(DataSourceUtil.getCatalogName(dataSource)).isNull();
  }
}
