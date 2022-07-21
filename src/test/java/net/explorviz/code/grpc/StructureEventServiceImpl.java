package net.explorviz.code.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import net.explorviz.code.proto.StructureCreateEvent;
import net.explorviz.code.proto.StructureDeleteEvent;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureModifyEvent;

@GrpcService
public class StructureEventServiceImpl implements StructureEventService {

  @Override
  public Uni<Empty> sendCreateEvent(final StructureCreateEvent request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Uni<Empty> sendDeleteEvent(final StructureDeleteEvent request) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Uni<Empty> sendModifyEvent(final StructureModifyEvent request) {
    // TODO Auto-generated method stub
    return null;
  }



}
