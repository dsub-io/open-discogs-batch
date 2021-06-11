package io.dsub.discogs.batch.testutil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public List<ILoggingEvent> getLogsByLevel(Level lv) throws TestInstantiationException {
    return getEvents().stream()
        .filter(log -> log.getLevel().isGreaterOrEqual(lv))
        .collect(Collectors.toList());
  }

  public List<ILoggingEvent> getLogsByLevelExact(Level lv) {
    return getEvents().stream()
        .filter(log -> log.getLevel().equals(lv))
        .collect(Collectors.toList());
  }

  public List<ILoggingEvent> getLogsByLevelExact(Level lv, String targetPackage) {
    return getEvents().stream()
        .filter(log -> log.getLevel().equals(lv))
        .filter(log -> log.getLoggerName().contains(targetPackage))
        .collect(Collectors.toList());
  }

  public List<String> getLogsAsString(boolean formatted) {
    return getEvents().stream()
        .map(log -> formatted ? log.getFormattedMessage() : log.getMessage())
        .collect(Collectors.toList());
  }

  public List<String> getLogsByLevelAsString(Level lv, boolean formatted) {
    return getLogsByLevel(lv).stream()
        .map(log -> formatted ? log.getFormattedMessage() : log.getMessage())
        .collect(Collectors.toList());
  }

  public List<String> getLogsByExactLevelAsString(Level lv, boolean formatted) {
    return getLogsByLevelExact(lv).stream()
        .map(log -> formatted ? log.getFormattedMessage() : log.getMessage())
        .collect(Collectors.toList());
  }

  public List<String> getLogsByExactLevelAsString(Level lv, boolean formatted, String basePackage) {
    return getLogsByLevelExact(lv, basePackage).stream()
        .map(log -> formatted ? log.getFormattedMessage() : log.getMessage())
        .collect(Collectors.toList());
  }

  public int count() {
    return getEvents().size();
  }

  public int count(Level level) {
    return getLogsByLevel(level).size();
  }

  public int countExact(Level level) {
    return getLogsByLevelExact(level).size();
  }

  public void clear() {
    appender.list.clear();
  }
}
