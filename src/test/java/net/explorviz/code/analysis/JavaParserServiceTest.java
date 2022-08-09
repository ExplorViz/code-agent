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
import javax.inject.Inject;
import net.explorviz.code.proto.MutinyStructureEventServiceGrpc;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureFileEvent;
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

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeToken; // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecret; // NOCS

  private Server server;

  private final MutinyStructureEventServiceGrpc.StructureEventServiceImplBase serviceImpl =
      mock(MutinyStructureEventServiceGrpc.StructureEventServiceImplBase.class,
          delegatesTo(new StructureEventService() {

            @Override
            public Uni<Empty> sendStructureFileEvent(final StructureFileEvent request) {
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
  void testProcessFile() throws IOException {

    final ArgumentCaptor<StructureFileEvent> requestCaptor =
        ArgumentCaptor.forClass(StructureFileEvent.class);

    this.parserService.processFile("src/test/resources/files/TestClass.java");

    verify(this.serviceImpl, times(1)).sendStructureFileEvent(requestCaptor.capture());

    final StructureFileEvent actual = requestCaptor.getValue();

    Assertions.assertEquals("files.TestClass", actual.getFullyQualifiedOperationName());

    Assertions.assertEquals(this.landscapeToken, actual.getLandscapeToken());

    Assertions.assertEquals(this.landscapeSecret, actual.getLandscapeSecret());
  }

}
