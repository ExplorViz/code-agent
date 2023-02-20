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

  @GrpcClient("codeAnalysisGrpcClient")
  /* package */ FileDataServiceGrpc.FileDataServiceBlockingStub fileDataGrpcClient; // NOCS
  //
  @GrpcClient("codeAnalysisGrpcClient")
  /* package */ CommitReportServiceGrpc.CommitReportServiceBlockingStub commitDataGrpcClient;// NOCS
  //
  @GrpcClient("codeAnalysisGrpcClient")
  /* package */ StateDataServiceGrpc.StateDataServiceBlockingStub stateDataGrpcClient; // NOCS

  @GrpcClient("codeAnalysisGrpcClient")
  /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS


  /**
   * Requests the state data from the remote endpoint.
   *
   * @param branchName the branch for the analysis
   * @return the state of the remote database
   */
  public StateData requestStateData(final String branchName) {
    StateDataRequest.Builder requestBuilder = StateDataRequest.newBuilder();
    requestBuilder.setBranchName(branchName);
    return stateDataGrpcClient.requestStateData(requestBuilder.build());
  }

  public void sendFileData(final FileData fileData) {
    fileDataGrpcClient.sendFileData(fileData);
  }

  @Override
  public void sendCommitReport(CommitReportData commitReportData) {
    commitDataGrpcClient.sendCommitReport(commitReportData);
  }
}
