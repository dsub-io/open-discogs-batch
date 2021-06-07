package io.dsub.discogs.batch.argument.formatter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JdbcUrlFormatterTest {

  JdbcUrlFormatter jdbcUrlFormatter = new JdbcUrlFormatter();

  @Test
  void format() {
    String format = jdbcUrlFormatter.format("--url=mysql://localhost/default");
    System.out.println(format);
  }
}