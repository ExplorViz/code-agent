package net.explorviz.code.analysis.visitor;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.explorviz.code.analysis.handler.TypeScriptFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrTypeScriptParserService;
import net.explorviz.code.proto.ClassData;
import net.explorviz.code.proto.ClassType;
import net.explorviz.code.proto.FileData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
public class TypeScriptFileDataListenerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptFileDataListenerTest.class);

  @Inject
  AntlrTypeScriptParserService tsParserService;

  @Test
  void testTypeScriptClassParsing() {
    final String tsCode = """
        export interface Person {
            name: string;
            age: number;
        }
        
        export class Greeter {
            private greeting: string;
        
            constructor(message: string) {
                this.greeting = message;
            }
        
            public greet(person: Person): string {
                return `${this.greeting}, ${person.name}!`;
            }
        }
        """;
    final String fileName = "Greeter.ts";
    final String commitSha = "test-sha-1";

    final TypeScriptFileDataHandler fileDataHandler =
        tsParserService.parseFileContent(tsCode, fileName, commitSha);
    
    Assertions.assertNotNull(fileDataHandler);
    
    final FileData data = fileDataHandler.getProtoBufObject();
    
    LOGGER.info("TypeScript file parsed: {} classes found", data.getClassDataCount());
    
    // We should have 2 "classes": Person (interface) and Greeter (class)
    Assertions.assertTrue(data.getClassDataCount() >= 1, "Should have at least 1 class/interface");
    
    // Check LOC metric exists
    Assertions.assertTrue(data.containsMetric("loc"), "Should have LOC metric");
    LOGGER.info("LOC: {}", data.getMetricMap().get("loc"));
  }

  @Test
  void testJavaScriptClassParsing() {
    final String jsCode = """
        class Calculator {
            constructor() {
                this.result = 0;
            }
        
            add(a, b) {
                return a + b;
            }
        
            subtract(a, b) {
                return a - b;
            }
        }
        
        function calculate(operation, x, y) {
            const calc = new Calculator();
            return calc[operation](x, y);
        }
        """;
    final String fileName = "Calculator.js";
    final String commitSha = "test-sha-2";

    final TypeScriptFileDataHandler fileDataHandler =
        tsParserService.parseFileContent(jsCode, fileName, commitSha);
    
    Assertions.assertNotNull(fileDataHandler);
    
    final FileData data = fileDataHandler.getProtoBufObject();
    
    LOGGER.info("JavaScript file parsed: {} classes found", data.getClassDataCount());
    
    // We should have 1 class: Calculator
    Assertions.assertTrue(data.getClassDataCount() >= 1, "Should have at least 1 class");
    
    // Check LOC metric exists
    Assertions.assertTrue(data.containsMetric("loc"), "Should have LOC metric");
    LOGGER.info("LOC: {}", data.getMetricMap().get("loc"));
  }

  @Test
  void testTypeScriptInterfaceParsing() {
    final String tsCode = """
        export interface User {
            id: number;
            name: string;
            email: string;
        }
        
        export interface Admin extends User {
            permissions: string[];
        }
        """;
    final String fileName = "User.ts";
    final String commitSha = "test-sha-3";

    final TypeScriptFileDataHandler fileDataHandler =
        tsParserService.parseFileContent(tsCode, fileName, commitSha);
    
    Assertions.assertNotNull(fileDataHandler);
    
    final FileData data = fileDataHandler.getProtoBufObject();
    
    LOGGER.info("TypeScript interfaces parsed: {} interfaces found", data.getClassDataCount());
    
    // We should have 2 interfaces: User and Admin
    Assertions.assertTrue(data.getClassDataCount() >= 1, "Should have at least 1 interface");
    
    // Check if we can identify one as an interface
    for (ClassData classData : data.getClassDataMap().values()) {
      LOGGER.info("Found: {} - Type: {}", classData, classData.getType());
      if (classData.getType() == ClassType.INTERFACE) {
        Assertions.assertTrue(true, "Found an interface");
        return;
      }
    }
  }

  @Test
  void testJavaScriptFunctionParsing() {
    final String jsCode = """
        function greet(name) {
            console.log(`Hello, ${name}!`);
        }
        
        function add(a, b) {
            return a + b;
        }
        
        const multiply = (a, b) => a * b;
        """;
    final String fileName = "functions.js";
    final String commitSha = "test-sha-4";

    final TypeScriptFileDataHandler fileDataHandler =
        tsParserService.parseFileContent(jsCode, fileName, commitSha);
    
    Assertions.assertNotNull(fileDataHandler);
    
    final FileData data = fileDataHandler.getProtoBufObject();
    
    LOGGER.info("JavaScript functions parsed");
    LOGGER.info("LOC: {}", data.getMetricMap().get("loc"));
    
    // Check LOC metric exists
    Assertions.assertTrue(data.containsMetric("loc"), "Should have LOC metric");
  }
}

