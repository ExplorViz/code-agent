package net.explorviz.code.analysis.parser;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.explorviz.code.analysis.handler.FileDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SrcmlParserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SrcmlParserService.class);
  private static final String SRCML_ENDPOINT = "http://localhost:8078/parse";

  private final HttpClient httpClient = HttpClient.newHttpClient();

  /**
   * Parses file content using the srcml HTTP API.
   *
   * @param fileContent  The raw source code
   * @param fileName     The file name (for logging or metadata)
   * @param commitSha    The commit SHA
   * @return A placeholder FileDataHandler (you can parse XML here later)
   */
  public FileDataHandler parseFileContent(String fileContent, String fileName, String commitSha) {
    String language = detectLanguage(fileName);

    String jsonRequest = String.format("""
    {
      "code": %s,
      "language": "%s"
    }
    """, toJsonString(fileContent), language);



    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(SRCML_ENDPOINT))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        String xml = response.body();

        // TODO: Parse XML into FileDataHandler, or add a new visitor/handler
        FileDataHandler data = new FileDataHandler(fileName);
        data.setCommitSha(commitSha);

        // Placeholder: log and return
        LOGGER.debug("Parsed file {} into XML ({})", fileName, xml);
        return data;
      } else {
        LOGGER.error("srcml API returned error: {}", response.body());
      }

    } catch (IOException | InterruptedException e) {
      LOGGER.error("Failed to call srcml API for {}", fileName, e);
    }

    return null;
  }


  private String detectLanguage(String fileName) {
    if (fileName == null) return "C++";

    String lowerName = fileName.toLowerCase();
    if (lowerName.endsWith(".java")) {
      return "Java";
    } else if (lowerName.endsWith(".cpp") || lowerName.endsWith(".cc") || lowerName.endsWith(".cxx") || lowerName.endsWith(".c")) {
      return "C++";
    }

    return "C++";
  }

  /**
   * Escapes a string for safe inclusion in JSON.
   */
  private String toJsonString(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        .replace("\r", "\\r") + "\"";
  }
}
