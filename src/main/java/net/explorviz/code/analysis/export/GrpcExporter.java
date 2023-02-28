package net.explorviz.code.analysis.export;

import io.quarkus.grpc.GrpcClient;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.CommitReportServiceGrpc;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.FileDataServiceGrpc;
import net.explorviz.code.proto.StateData;
import net.explorviz.code.proto.StateDataRequest;
import net.explorviz.code.proto.StateDataServiceGrpc;
import net.explorviz.code.proto.StructureEventServiceGrpc;

/**
 * Basic GRPC handler.
 */
@ApplicationScoped
public final class GrpcExporter implements DataExporter {
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


  /**
   * Requests the state data from the remote endpoint.
   *
   * @param branchName the branch for the analysis
   * @return the state of the remote database
   */
  @Override
  public StateData requestStateData(final String branchName) {
    final StateDataRequest.Builder requestBuilder = StateDataRequest.newBuilder();
    requestBuilder.setBranchName(branchName);
    return stateDataGrpcClient.requestStateData(requestBuilder.build());
  }

  @Override
  public void sendFileData(final FileData fileData) {
    fileDataGrpcClient.sendFileData(fileData);
  }

  @Override
  public void sendCommitReport(final CommitReportData commitReportData) {
    commitDataGrpcClient.sendCommitReport(commitReportData);
  }

  @Override
  public boolean isInvalidCommitHash(final String hash) {
    return "0000000000000000000000000000000000000000".equals(hash);
  }
}
