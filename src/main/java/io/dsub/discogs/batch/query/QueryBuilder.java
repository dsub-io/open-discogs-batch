package io.dsub.discogs.batch.query;

public interface QueryBuilder<T> {

  String SPACE = " ";
  String PERIOD = ".";
  String COMMA = ",";
  String AND = "AND";
  String COLON = ":";
  String PLUS = "+";
  String SEMICOLON = ";";
  String OPEN_BRACE = "(";
  String CLOSE_BRACE = ")";
  String TMP = "_tmp";
  String EQUALS = "=";

  String getTemporaryInsertQuery(Class<? extends T> targetClass);

  String getSelectInsertQuery(Class<? extends T> targetClass);

  String getPruneQuery(Class<? extends T> targetClass);

  String getIdOnlyInsertQuery(Class<? extends T> targetClass);
}
