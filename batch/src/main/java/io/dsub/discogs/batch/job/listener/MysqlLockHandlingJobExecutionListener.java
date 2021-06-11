package io.dsub.discogs.batch.job.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class MysqlLockHandlingJobExecutionListener extends LockHandlingJobExecutionListener {

  private static final String DISABLE_FK_CHK_SQL = "SET FOREIGN_KEY_CHECKS=0";

  private static final String ENABLE_FK_CHK_SQL = "SET FOREIGN_KEY_CHECKS=1";

  private final JdbcTemplate jdbcTemplate;

  @Override
  protected void disableConstraints() {
    jdbcTemplate.execute(DISABLE_FK_CHK_SQL);
  }

  @Override
  protected void enableConstraints() {
    jdbcTemplate.execute(ENABLE_FK_CHK_SQL);
  }
}
