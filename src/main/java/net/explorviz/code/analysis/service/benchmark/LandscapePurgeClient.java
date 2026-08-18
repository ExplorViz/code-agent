package net.explorviz.code.analysis.service.benchmark;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client that requests a chunked database purge from the landscape service.
 */
@ApplicationScoped
public class LandscapePurgeClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(LandscapePurgeClient.class);
  private static final int DEFAULT_CHUNK_SIZE = 10_000;
  private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(30);

  @ConfigProperty(name = "explorviz.landscape.rest.host", defaultValue = "127.0.0.1")
  /* default */ String landscapeRestHost;

  @ConfigProperty(name = "explorviz.landscape.rest.port", defaultValue = "8085")
  /* default */ int landscapeRestPort;

  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  public void purgeDatabase() throws IOException, InterruptedException {
    purgeDatabase(DEFAULT_CHUNK_SIZE);
  }

  public void purgeDatabase(final int chunkSize) throws IOException, InterruptedException {
    final URI uri =
        URI.create(
            String.format(
                "http://%s:%d/api/benchmark/purge-database?chunkSize=%d",
                landscapeRestHost, landscapeRestPort, chunkSize));

    LOGGER.info("Requesting landscape database purge at {}", uri);

    final HttpRequest request =
        HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).timeout(REQUEST_TIMEOUT).build();

    final HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(
          "Landscape purge failed with status "
              + response.statusCode()
              + ": "
              + response.body());
    }

    LOGGER.info("Landscape database purge completed: {}", response.body());
  }
}
