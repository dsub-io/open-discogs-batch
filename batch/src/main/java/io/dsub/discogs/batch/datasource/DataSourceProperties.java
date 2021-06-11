package io.dsub.discogs.batch.datasource;

public interface DataSourceProperties {
  String getUsername();

  String getPassword();

  String getConnectionUrl();

  DBType getDbType();
}
