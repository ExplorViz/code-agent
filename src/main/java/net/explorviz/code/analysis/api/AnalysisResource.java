package net.explorviz.code.analysis.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.export.GrpcExporter;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.service.AnalysisConfig;
import net.explorviz.code.analysis.service.AnalysisService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for triggering Git analysis operations.
 */
@Path("/api/analysis")
public class AnalysisResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResource.class);

  @ConfigProperty(name = "explorviz.gitanalysis.send-to-remote", defaultValue = "true")
  /* default */ boolean sendToRemoteProperty; // NOCS

  @Inject
  /* default */ AnalysisService analysisService; // NOCS

  @Inject
  /* default */ GrpcExporter grpcExporter; // NOCS

  /**
   * Triggers a Git repository analysis with the provided configuration.
   *
   * @param request The analysis request containing configuration
   * @return Response indicating success or failure
   */
  @POST
  @Path("/trigger")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public Response triggerAnalysis(final AnalysisRequest request) {
    if (request == null) {
      LOGGER.error("Request body is null or invalid");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Request body is required")
          .build();
    }

    try {
      final String repoInfo = request.getRepoPath() != null ? request.getRepoPath()
          : (request.getRepoRemoteUrl() != null ? request.getRepoRemoteUrl() : "unknown");
      LOGGER.info("Triggering analysis for repository: {}", repoInfo);

      final AnalysisConfig config = request.toConfig();

      final DataExporter exporter;
      if (request.isSendToRemote()) {
        exporter = grpcExporter;
      } else {
        exporter = new JsonExporter(config.getApplicationName());
      }

      analysisService.analyzeAndSendRepo(config, exporter);

      LOGGER.info("Analysis completed successfully");
      return Response.ok("Analysis completed successfully").build();

    } catch (IOException | GitAPIException | NotFoundException | PropertyNotDefinedException e) {
      LOGGER.error("Analysis failed: {}", e.getMessage(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Analysis failed: " + e.getMessage())
          .build();
    }
  }
}
