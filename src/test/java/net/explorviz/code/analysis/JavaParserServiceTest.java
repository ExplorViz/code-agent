package net.explorviz.code.analysis;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.google.protobuf.Empty;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.inject.Inject;
import net.explorviz.code.proto.MutinyStructureEventServiceGrpc;
import net.explorviz.code.proto.StructureCreateEvent;
import net.explorviz.code.proto.StructureDeleteEvent;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureModifyEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Rule;
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

  @ConfigProperty(name = "quarkus.grpc.clients.\"StructureEventService\".port")
  int port;

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

    // server
    this.grpcCleanup
        .register(NettyServerBuilder.forAddress(new InetSocketAddress("localhost", this.port))
            .directExecutor().addService(this.serviceImpl).build().start());

    // channel
    // this.grpcCleanup.register(NettyChannelBuilder
    // .forAddress(new InetSocketAddress("localhost", port)).usePlaintext().build());
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

    this.parserService.processFile("src/test/resources/files/TestClass.java");

    verify(this.serviceImpl, times(1)).sendModifyEvent(any());

    // Assertions.assertTrue(true);

  }

}
