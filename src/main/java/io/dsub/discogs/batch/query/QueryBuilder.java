package io.dsub.discogs.batch.query;

public interface QueryBuilder<T> {

  String SPACE = " ";
  String PERIOD = ".";
  String COMMA = ",";
  String AND = "AND";
  String INSERT = "INSERT";
  String INTO = "INTO";
  String COLON = ":";
  String PLUS = "+";
  String SEMICOLON = ";";
  String SELECT = "SELECT";
  String OPEN_BRACE = "(";
  String CLOSE_BRACE = ")";
  String TMP = "_tmp";
  String FROM = "FROM";
  String EQUALS = "=";
  String DELETE = "DELETE";
  String INSERT_INTO = INSERT + SPACE + INTO;
  String DELETE_FROM = DELETE + SPACE + FROM;

  String getTemporaryInsertQuery(Class<? extends T> targetClass);

  String getSelectInsertQuery(Class<? extends T> targetClass);

  String getPruneQuery(Class<? extends T> targetClass);
}
