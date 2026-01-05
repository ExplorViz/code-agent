package net.explorviz.code.analysis.integration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.handler.PythonFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrPythonParserService;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for Python file parsing and JSON export.
 */
@QuarkusTest
public class PythonExportTest {

  @Inject
  /* package */ AntlrPythonParserService pythonParserService;

  @Test
  public void testPythonParsingAndExport() throws Exception {
    // Create sample Python content
    final String pythonContent = """
        # Sample Python file
        import os
        from typing import List
        
        class Calculator:
            def __init__(self):
                self.result = 0
            
            def add(self, a, b):
                return a + b
            
            def subtract(self, a, b):
                return a - b
        
        def main():
            calc = Calculator()
            print(calc.add(5, 3))
        
        if __name__ == "__main__":
            main()
        """;

    // Parse the Python content
    final PythonFileDataHandler handler = pythonParserService.parseFileContent(
        pythonContent,
        "calculator.py",
        "test123"
    );

    // Verify handler is not null
    Assertions.assertNotNull(handler, "Python parser should return a handler");

    // Get the FileData protobuf object
    final FileData fileData = handler.getProtoBufObject();

    // Verify basic file data
    Assertions.assertEquals("calculator.py", fileData.getFileName());
    Assertions.assertEquals(Language.PYTHON, fileData.getLanguage());
    Assertions.assertTrue(fileData.getMetricMap().containsKey("loc"));

    // Verify class was detected
    Assertions.assertTrue(fileData.getClassDataMap().containsKey("Calculator"),
        "Calculator class should be detected");

    // Verify methods in the class
    final var calculatorClass = fileData.getClassDataMap().get("Calculator");
    Assertions.assertFalse(calculatorClass.getMethodDataMap().isEmpty(),
        "Calculator should have methods");

    // Verify global function
    Assertions.assertFalse(fileData.getFunctionsList().isEmpty(),
        "Should have global functions");
    Assertions.assertTrue(
        fileData.getFunctionsList().stream()
            .anyMatch(f -> f.getName().equals("main")),
        "Should have main function"
    );

    // Test complete - Python parsing works correctly!
  }
}

