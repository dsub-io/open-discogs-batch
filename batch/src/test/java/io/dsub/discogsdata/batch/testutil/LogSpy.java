package io.dsub.discogsdata.batch.testutil;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LogSpy implements BeforeEachCallback, AfterEachCallback {

  private ch.qos.logback.classic.Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @Override
  public void afterEach(ExtensionContext context) {
    logger.detachAppender(appender);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    appender = new ListAppender<>();
    logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.addAppender(appender);
    appender.start();
  }

  public List<ILoggingEvent> getEvents() throws TestInstantiationException {
    if (appender == null) {
      throw new TestInstantiationException("LogSpy needs to be annotated with @Rule");
    }
    return appender.list;
  }
}
