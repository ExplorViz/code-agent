package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.StateData;

/**
 * Basic GRPC handler.
 */
public final class GrpcHandler {

  private GrpcHandler() {
  }

  /**
   * Requests the state data from the remote endpoint.
   *
   * @param branchName the branch for the analysis
   * @return the state of the remote database
   */
  public static StateData requestStateData(final String branchName) {
    // throw new ExecutionControl.NotImplementedException("Currently not Implemented");

    // MOCKING SOME DATA
    final StateData.Builder builder = StateData.newBuilder();
    builder.setBranchName(branchName);
    builder.setCommitID("");
    return builder.build();
  }
}
