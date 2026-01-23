package net.explorviz.code.analysis.integration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.explorviz.code.analysis.handler.PythonFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrPythonParserService;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
                                "test123");

                // Verify handler is not null
                Assertions.assertNotNull(handler, "Python parser should return a handler");

                // Get the FileData protobuf object
                final FileData fileData = handler.getProtoBufObject();

                // Verify basic file data
                Assertions.assertEquals("calculator.py", fileData.getFilePath());
                Assertions.assertEquals(Language.PYTHON, fileData.getLanguage());
                Assertions.assertTrue(fileData.containsMetrics("loc"));

                // Verify class was detected with correct FQN format (filePath:ClassName)
                final String expectedCalculatorFqn = "Calculator";
                Assertions.assertTrue(
                                fileData.getClassesList().stream()
                                                .anyMatch(c -> c.getName().equals(expectedCalculatorFqn)),
                                "Calculator class should be detected");

                // Verify methods in the class
                final var calculatorClass = fileData.getClassesList().stream()
                                .filter(c -> c.getName().equals(expectedCalculatorFqn)).findFirst().orElseThrow();
                Assertions.assertFalse(calculatorClass.getFunctionsList().isEmpty(),
                                "Calculator should have methods");

                // Verify global function
                Assertions.assertFalse(fileData.getFunctionsList().isEmpty(),
                                "Should have global functions");

                final String expectedMainFqn = "main";
                Assertions.assertTrue(
                                fileData.getFunctionsList().stream()
                                                .anyMatch(f -> f.getName().equals(expectedMainFqn)),
                                "Should have main function");
        }

        private void assertEquals(String expected, String actual) {
                Assertions.assertEquals(expected, actual);
        }
}
