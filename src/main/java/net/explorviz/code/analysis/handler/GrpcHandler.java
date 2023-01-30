package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.StateData;

/**
 * Basic GRPC handler.
 */
public class GrpcHandler {
  /**
   * Requests the state data from the remote endpoint.
   *
   * @param branchName the branch for the analysis
   * @return the state of the remote database
   */
  public static StateData requestStateData(String branchName) {
    // throw new ExecutionControl.NotImplementedException("Currently not Implemented");

    // MOCKING SOME DATA
    StateData.Builder builder = StateData.newBuilder();
    builder.setBranchName(branchName);
    builder.setCommitID("");
    return builder.build();
  }
}
