package net.explorviz.code.analysis;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import javax.inject.Inject;
import net.explorviz.code.proto.StructureCreateEvent;
import net.explorviz.code.proto.StructureDeleteEvent;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureEventServiceGrpc;
import net.explorviz.code.proto.StructureModifyEvent;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Collects class names.
 */
@QuarkusTest
public class JavaParserServiceTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Inject
  JavaParserService parserService;

  @BeforeEach
  void setup() throws IOException {

    // TODO GrpcClient Channel override fpr JavaParserService

    // this.channel = ManagedChannelBuilder.forAddress("localhost", 9001).usePlaintext().build();

    final StructureEventServiceGrpc.StructureEventServiceImplBase serviceImpl =
        mock(StructureEventServiceGrpc.StructureEventServiceImplBase.class,
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

    // Create a server, add service, start, and register for automatic graceful shutdown.

    final Server server = ServerBuilder.forPort(9001).addService(serviceImpl).build().start();

    // final Server server = InProcessServerBuilder.forName("localhost").directExecutor()
    // .addService(serviceImpl).build().start();

    this.grpcCleanup.register(server);

    // System.out.println(server.getListenSockets());

    // MutinyStructureEventServiceGrpc.newMutinyStub(server.get);

  }

  private CompilationUnit createUnit() {
    final JavaParser javaParser = new JavaParser();

    final CompilationUnit unit =
        javaParser.parse("public class Test\n" + "{\n" + "   public class InnerTest\n" + "   {\n"
            + "       public InnerTest() {}\n" + "   }\n" + "    \n" + "   public Test() {\n"
            + "   }\n" + "\n" + "   public static void main( String[] args ) { \n"
            + "       new Test().new InnerTest();\n" + "   }\n" + "}").getResult().get();
    return unit;
  }

  @Test()
  void testPayload() throws IOException {

    Assertions.assertTrue(true);

  }

}
