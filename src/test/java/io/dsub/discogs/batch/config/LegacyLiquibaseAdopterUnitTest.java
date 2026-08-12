package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyLiquibaseAdopterUnitTest {

  @Test
  void failsClosedWhenLegacyLockTableInspectionReturnsNoRow() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet result = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(result);
    when(result.next()).thenReturn(false);
    LegacyLiquibaseContract contract =
        new LegacyLiquibaseContract(List.of(), List.of());

    assertThatThrownBy(
            () ->
                new LegacyLiquibaseAdopter(new DatabaseSchema("open_discogs"))
                    .adopt(connection, contract, List.of(), 0))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("table inspection returned no result");
  }
}
