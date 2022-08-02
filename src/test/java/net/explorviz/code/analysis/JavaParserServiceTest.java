package net.explorviz.code.analysis;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import javax.inject.Inject;
import net.explorviz.code.proto.MutinyStructureEventServiceGrpc;
import net.explorviz.code.proto.StructureCreateEvent;
import net.explorviz.code.proto.StructureDeleteEvent;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureModifyEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Collects class names.
 */
@QuarkusTest
public class JavaParserServiceTest {

  @Inject
  JavaParserService parserService;

  @ConfigProperty(name = "quarkus.grpc.clients.\"StructureEventService\".port")
  int port;

  private Server server;

  private final MutinyStructureEventServiceGrpc.StructureEventServiceImplBase serviceImpl =
      mock(MutinyStructureEventServiceGrpc.StructureEventServiceImplBase.class,
          delegatesTo(new StructureEventService() {

            @Override
            public Uni<Empty> sendCreateEvent(final StructureCreateEvent request) {
              return Uni.createFrom().item(() -> Empty.newBuilder().build());
            }

            @Override
            public Uni<Empty> sendDeleteEvent(final StructureDeleteEvent request) {
              return Uni.createFrom().item(() -> Empty.newBuilder().build());
            }

            @Override
            public Uni<Empty> sendModifyEvent(final StructureModifyEvent request) {
              return Uni.createFrom().item(() -> Empty.newBuilder().build());
            }
          }));

  @BeforeEach
  void setup() throws IOException {

    this.server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", this.port))
        .directExecutor().addService(this.serviceImpl).build().start();

    // channel
    // this.grpcCleanup.register(NettyChannelBuilder
    // .forAddress(new InetSocketAddress("localhost", port)).usePlaintext().build());
  }

  @AfterEach
  void cleanup() {
    this.server.shutdownNow();
  }

  @Test()
  void testProcessFolder() throws IOException {

    final ArgumentCaptor<StructureCreateEvent> requestCaptor =
        ArgumentCaptor.forClass(StructureCreateEvent.class);

    this.parserService.processFolder("src/test/resources/files");

    verify(this.serviceImpl, times(2)).sendCreateEvent(requestCaptor.capture());

    final List<StructureCreateEvent> actuals = requestCaptor.getAllValues();

    Assertions.assertEquals(2, actuals.size());

    Assertions.assertEquals("files.TestClass", actuals.get(0).getFullyQualifiedOperationName());
    Assertions.assertEquals("files.TestClass2", actuals.get(1).getFullyQualifiedOperationName());
  }

  @Test()
  void testProcessFile() throws IOException {

    final ArgumentCaptor<StructureModifyEvent> requestCaptor =
        ArgumentCaptor.forClass(StructureModifyEvent.class);

    this.parserService.processFile("src/test/resources/files/TestClass.java");

    verify(this.serviceImpl, times(1)).sendModifyEvent(requestCaptor.capture());

    Assertions.assertEquals("files.TestClass",
        requestCaptor.getValue().getFullyQualifiedOperationName());
  }

}
