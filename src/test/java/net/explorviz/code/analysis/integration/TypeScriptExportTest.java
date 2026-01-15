package net.explorviz.code.analysis.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.handler.TypeScriptFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrTypeScriptParserService;
import net.explorviz.code.proto.FileData;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify TypeScript/JavaScript files are parsed and exported correctly.
 */
public class TypeScriptExportTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptExportTest.class);

  @Test
  public void testTypeScriptFileExport() throws IOException {
    LOGGER.info("=== Starting TypeScript Export Test ===");
    
    // Step 1: Create a simple TypeScript file content
    final String tsContent = """
      class Calculator {
        add(a: number, b: number): number {
          return a + b;
        }
      }
      
      function multiply(x: number, y: number): number {
        return x * y;
      }
      
      export { Calculator, multiply };
      """;
    
    LOGGER.info("✅ Step 1: Created test TypeScript content");
    
    // Step 2: Parse the TypeScript content
    final AntlrTypeScriptParserService parser = new AntlrTypeScriptParserService();
    final TypeScriptFileDataHandler handler = parser.parseFileContent(
        tsContent, 
        "test.ts", 
        "test-commit-123"
    );
    
    assertNotNull(handler, "❌ Parser returned NULL - parsing failed!");
    LOGGER.info("✅ Step 2: Parser returned a handler");
    
    // Step 3: Get the protobuf object
    final FileData fileData = handler.getProtoBufObject();
    assertNotNull(fileData, "❌ FileData is NULL");
    LOGGER.info("✅ Step 3: Got FileData protobuf object");
    
    // Step 4: Verify the data
    assertEquals("test.ts", fileData.getFileName());
    assertEquals("test-commit-123", fileData.getCommitID());
    assertEquals("TYPESCRIPT", fileData.getLanguage().toString());
    
    LOGGER.info("   - File name: {}", fileData.getFileName());
    LOGGER.info("   - Commit ID: {}", fileData.getCommitID());
    LOGGER.info("   - Language: {}", fileData.getLanguage());
    LOGGER.info("   - Classes: {}", fileData.getClassDataCount());
    LOGGER.info("   - Functions: {}", fileData.getFunctionsCount());
    LOGGER.info("   - Exports: {}", fileData.getExportsCount());
    
    assertTrue(fileData.getClassDataCount() > 0, "Should have detected Calculator class");
    
    // Verify FQN format for classes (filePath:ClassName)
    final String expectedCalculatorFqn = "test.ts:Calculator";
    assertTrue(fileData.getClassDataMap().containsKey(expectedCalculatorFqn),
        "Calculator class should have FQN: " + expectedCalculatorFqn);
    
    // Verify FQN format for global functions (filePath:functionName)
    final String expectedMultiplyFqn = "test.ts:multiply";
    assertTrue(
        fileData.getFunctionsList().stream()
            .anyMatch(f -> f.getName().equals("multiply") && f.getFqn().equals(expectedMultiplyFqn)),
        "multiply function should have FQN: " + expectedMultiplyFqn
    );
    
    // Step 5: Export to JSON
    final String tempDir = System.getProperty("java.io.tmpdir");
    final JsonExporter exporter = new JsonExporter(tempDir);
    
    LOGGER.info("   - Export directory: {}", tempDir);
    
    exporter.sendFileData(fileData);
    LOGGER.info("✅ Step 5: Exported to JSON");
    
    // Step 6: Verify the JSON file was created
    final String expectedFileName = "test_test-commit-123.json";
    final String expectedPath = Paths.get(tempDir, expectedFileName).toString();
    
    assertTrue(Files.exists(Paths.get(expectedPath)), 
        "❌ JSON file was NOT created at: " + expectedPath);
    
    LOGGER.info("✅ Step 6: JSON file created at: {}", expectedPath);
    
    // Step 7: Read and verify the JSON content
    final String jsonContent = Files.readString(Paths.get(expectedPath));
    assertTrue(jsonContent.contains("\"fileName\": \"test.ts\""));
    assertTrue(jsonContent.contains("\"language\": \"TYPESCRIPT\""));
    
    LOGGER.info("✅ Step 7: JSON content is valid");
    LOGGER.info("=== TypeScript Export Test PASSED ===");
    
    // Cleanup
    Files.deleteIfExists(Paths.get(expectedPath));
  }
  
  @Test
  public void testJavaScriptFileExport() throws IOException {
    LOGGER.info("=== Starting JavaScript Export Test ===");
    
    final String jsContent = """
      class Counter {
        constructor() {
          this.count = 0;
        }
        increment() {
          this.count++;
        }
      }
      
      function reset() {
        return 0;
      }
      """;
    
    final AntlrTypeScriptParserService parser = new AntlrTypeScriptParserService();
    final TypeScriptFileDataHandler handler = parser.parseFileContent(
        jsContent, 
        "counter.js", 
        "test-commit-456"
    );
    
    assertNotNull(handler, "❌ Parser returned NULL for JavaScript");
    
    final FileData fileData = handler.getProtoBufObject();
    assertEquals("JAVASCRIPT", fileData.getLanguage().toString());
    
    LOGGER.info("✅ JavaScript file parsed successfully");
    LOGGER.info("   - Language: {}", fileData.getLanguage());
    LOGGER.info("   - Classes: {}", fileData.getClassDataCount());
    LOGGER.info("   - Functions: {}", fileData.getFunctionsCount());
    
    // Verify FQN format for JavaScript classes and functions
    final String expectedCounterFqn = "counter.js:Counter";
    assertTrue(fileData.getClassDataMap().containsKey(expectedCounterFqn),
        "Counter class should have FQN: " + expectedCounterFqn);
    
    final String expectedResetFqn = "counter.js:reset";
    assertTrue(
        fileData.getFunctionsList().stream()
            .anyMatch(f -> f.getName().equals("reset") && f.getFqn().equals(expectedResetFqn)),
        "reset function should have FQN: " + expectedResetFqn
    );
    
    LOGGER.info("   - Class FQN: {}", expectedCounterFqn);
    LOGGER.info("   - Function FQN: {}", expectedResetFqn);
    
    // Export
    final String tempDir = System.getProperty("java.io.tmpdir");
    final JsonExporter exporter = new JsonExporter(tempDir);
    exporter.sendFileData(fileData);
    
    final String expectedFileName = "counter_test-commit-456.json";
    final String expectedPath = Paths.get(tempDir, expectedFileName).toString();
    
    assertTrue(Files.exists(Paths.get(expectedPath)), 
        "❌ JSON file was NOT created for JavaScript");
    
    LOGGER.info("✅ JavaScript file exported to: {}", expectedPath);
    LOGGER.info("=== JavaScript Export Test PASSED ===");
    
    // Cleanup
    Files.deleteIfExists(Paths.get(expectedPath));
  }
}

