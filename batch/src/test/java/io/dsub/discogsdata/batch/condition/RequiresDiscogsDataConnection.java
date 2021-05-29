package io.dsub.discogsdata.batch.condition;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class RequiresDiscogsDataConnection implements ExecutionCondition {

  private static final String DISCOGS_DATA_URL = "https://data.discogs.com";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx) {
    InputStream in = null;
    try {
      final URL url = new URL(DISCOGS_DATA_URL);
      final URLConnection conn = url.openConnection();
      conn.connect();
      in = conn.getInputStream();
    } catch (MalformedURLException e) {
      // throw runtime exception as malformed url is an unacceptable fault.
      throw new RuntimeException(e);
    } catch (IOException e) {
      return ConditionEvaluationResult.disabled("failed to connect to URL: " + DISCOGS_DATA_URL);
    } finally {
      if (in != null) {
        try {
          in.close();
          in = null;
        } catch (IOException ignored) {
          // ignored
        }
      }
    }
    return ConditionEvaluationResult.enabled("successfully connected to URL:" + DISCOGS_DATA_URL);
  }
}
