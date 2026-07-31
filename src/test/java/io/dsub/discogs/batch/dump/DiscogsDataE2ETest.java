package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

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

  private static final URI CHECKSUM_MANIFEST =
      URI.create(
          "https://discogs-data-dumps.s3.us-west-2.amazonaws.com/"
              + "data/2026/discogs_20260701_CHECKSUM.txt");

  @Test
  void publicChecksumManifestContainsTheCompleteMonthlyDumpSet() throws Exception {
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(CHECKSUM_MANIFEST)
            .timeout(Duration.ofSeconds(30))
            .header(
                "User-Agent",
                "OpenDiscogsBatch-E2E/1.0"
                    + " (+https://github.com/dsub-io/open-discogs-batch)")
            .GET()
            .build();

    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    assertThat(response.statusCode())
        .withFailMessage(
            "Discogs checksum manifest returned HTTP %s: %s",
            response.statusCode(),
            response.body())
        .isEqualTo(200);
    assertThat(response.body())
        .contains(
            "discogs_20260701_artists.xml.gz",
            "discogs_20260701_labels.xml.gz",
            "discogs_20260701_masters.xml.gz",
            "discogs_20260701_releases.xml.gz");
  }
}
