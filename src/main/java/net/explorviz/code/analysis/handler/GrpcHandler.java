package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.FileData;
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

  public static void sendFileData(final FileData fileData) {
    // TODO: enable GRPC again
    // for (int i = 0; i < classes.size(); i++) {
    //   final StructureFileEvent event = classes.get(i);
    //   final StructureFileEvent eventWithTiming = StructureFileEvent.newBuilder(event)
    //       .setEpochMilli(authorIdent.getWhen().getTime()).build();
    //   classes.set(i, eventWithTiming);
    //   // grpcClient.sendStructureFileEvent(event).await().indefinitely();
    //   grpcClient.sendStructureFileEvent(event);
    // }

    // if (LOGGER.isDebugEnabled()) {
    //   LOGGER.debug("Classes names: {}", classes);
    // }
  }
}
