package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class DiscogsDataE2ETest {

  private static final URI DUMP_OBJECT =
      URI.create(
          "https://discogs-data-dumps.s3.us-west-2.amazonaws.com/"
              + "data/2026/discogs_20260701_releases.xml.gz");

  @Test
  void publicDumpObjectServesGzipBytes() throws Exception {
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(DUMP_OBJECT)
            .timeout(Duration.ofSeconds(30))
            .header("Range", "bytes=0-1")
            .header(
                "User-Agent",
                "OpenDiscogsBatch-E2E/1.0"
                    + " (+https://github.com/dsub-io/open-discogs-batch)")
            .GET()
            .build();

    HttpResponse<InputStream> response =
        client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    byte[] body;
    try (InputStream input = response.body()) {
      body = input.readNBytes(response.statusCode() == 206 ? 2 : 4096);
    }

    assertThat(response.statusCode())
        .withFailMessage(
            "Discogs dump object returned HTTP %s: %s",
            response.statusCode(),
            new String(body, StandardCharsets.UTF_8))
        .isEqualTo(206);
    assertThat(response.headers().firstValue("Content-Range").orElse(""))
        .startsWith("bytes 0-1/");
    assertThat(body).containsExactly((byte) 0x1f, (byte) 0x8b);
  }
}
