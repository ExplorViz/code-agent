package net.explorviz.code.analysis.handler;

import jdk.jshell.spi.ExecutionControl;
import net.explorviz.code.proto.StateData;

public class GrpcHandler {
  public static StateData requestStateData(String branchName)
      throws ExecutionControl.NotImplementedException {
    // throw new ExecutionControl.NotImplementedException("Currently not Implemented");

    // MOCKING SOME DATA
    StateData.Builder builder = StateData.newBuilder();
    builder.setBranchName(branchName);
    builder.setCommitID("");
    return builder.build();
  }
}
