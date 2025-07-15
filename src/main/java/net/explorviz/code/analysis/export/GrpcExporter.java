package net.explorviz.code.analysis.export;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.CommitReportServiceGrpc;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.FileDataServiceGrpc;
import net.explorviz.code.proto.StateData;
import net.explorviz.code.proto.StateDataRequest;
import net.explorviz.code.proto.StateDataServiceGrpc;
import net.explorviz.code.proto.StructureEventServiceGrpc;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic GRPC handler.
 */
@ApplicationScoped
public final class GrpcExporter implements DataExporter {

  public static final Logger LOGGER = LoggerFactory.getLogger(GrpcExporter.class);

  private static final String GRPC_CLIENT_NAME = "codeAnalysisGrpcClient";

  @GrpcClient(GRPC_CLIENT_NAME)
  /* package */ FileDataServiceGrpc.FileDataServiceBlockingStub fileDataGrpcClient; // NOCS
  //
  @GrpcClient(GRPC_CLIENT_NAME)
  /* package */ CommitReportServiceGrpc.CommitReportServiceBlockingStub commitDataGrpcClient;// NOCS
  //
  @GrpcClient(GRPC_CLIENT_NAME)
  /* package */ StateDataServiceGrpc.StateDataServiceBlockingStub stateDataGrpcClient; // NOCS

  @GrpcClient(GRPC_CLIENT_NAME)
  /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeTokenProperty;  // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecretProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.application-name")
  /* default */ String applicationNameProperty;  // NOCS

  /**
   * Requests the state data from the remote endpoint.
   *
   * @param branchName the branch for the analysis
   * @return the state of the remote database
   */
  @Override
  public StateData requestStateData(final String upstreamName, final String branchName) {
    final StateDataRequest.Builder requestBuilder = StateDataRequest.newBuilder();
    requestBuilder.setBranchName(branchName);
    requestBuilder.setUpstreamName(upstreamName);
    requestBuilder.setLandscapeToken(landscapeTokenProperty);
    requestBuilder.setLandscapeSecret(landscapeSecretProperty);
    requestBuilder.setApplicationName(applicationNameProperty);
    return stateDataGrpcClient.requestStateData(requestBuilder.build());
  }

  @Override
  public void sendFileData(final FileData fileData) {
    try {
      fileDataGrpcClient.sendFileData(fileData);
    } catch (final Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Failed to send file data {}", fileData);
        LOGGER.info(e.getMessage());
      }
    }
  }

  @Override
  public void sendCommitReport(final CommitReportData commitReportData) {
    LOGGER.info("Sending commit report on {}", commitReportData.getCommitID());
    try {
      commitDataGrpcClient.sendCommitReport(commitReportData);
    } catch (final Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Failed to send commit report {}", commitReportData);
        LOGGER.error(e.getMessage());
      }
    }

  }

  @Override
  public boolean isInvalidCommitHash(final String hash) {
    return "0000000000000000000000000000000000000000".equals(hash);
  }
}
