package com.gel.testgen;

import com.gel.driver.namingstrategies.NamingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final Path TEST_DEFINITION_DIRECTORY = Path.of("src", "driver", "src", "test", "java", "shared", "testdefs");
    private static final Path TEST_OUTPUT_DIRECTORY = Path.of("src", "driver", "src", "test", "java", "shared", "generated");

    private static final NamingStrategy PASCAL_CASE_NAMING_STRATEGY = NamingStrategy.pascalCase();
    private static final NamingStrategy CAMEL_CASE_NAMING_STRATEGY = NamingStrategy.camelCase();

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public static void main(String[] args) throws IOException {
        var dir =  Path.of(System.getProperty("user.dir"));

        var definitionDir = dir.resolve(TEST_DEFINITION_DIRECTORY).toFile();

        var files = definitionDir.listFiles((d,e) -> d.getAbsolutePath() == definitionDir.getAbsolutePath() && e.endsWith(".json"));

        if(files == null) {
            throw new FileNotFoundException("No files in definition directory");
        }

        for(var file : files) {
            var group = mapper.readValue(Files.readString(file.toPath(), StandardCharsets.UTF_8), TestGroup.class);
            var testFiles = dir.resolve(TEST_DEFINITION_DIRECTORY).resolve(file.getName().replace(".json", "")).toFile().listFiles();
            processGroup(group, testFiles);
        }
    }

    private static final String[] usings = new String[] {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.DisplayName",
            "shared.SharedTestsRunner",
            "java.nio.file.Path"
    };

    private static void processGroup(TestGroup group, File[] tests) throws IOException {
        var writer = new CodeWriter();

        writer.appendLine("package shared.generated;");

        for(var using : usings) {
            writer.append("import ");
            writer.append(using);
            writer.appendLine(";");
        }

        var pascalGroupName = PASCAL_CASE_NAMING_STRATEGY.convert(group.name).replace(" ", "");
        var camelCaseName = CAMEL_CASE_NAMING_STRATEGY.convert(group.name).replace(" ", "");

        writer.append("public class ");
        try (var classScope = writer.beginScope(pascalGroupName + "Tests")) {
            for(var testFile : tests) {
                var test = readTest(testFile);
                writer.appendLine("@Test");
                writer.appendLine("@DisplayName(\"" + test.name + "\")");

                try (var methodScope = writer.beginScope("public void " + camelCaseName + "_" + testFile.getName().replace(".json", "") + "()")) {
                    writer.appendLine("var path = Path.of(\"" + TEST_DEFINITION_DIRECTORY.resolve(testFile.getParentFile().getName()).resolve(testFile.getName()).toString().replace(System.getProperty("file.separator"), "\", \"") + "\");");
                    writer.appendLine("SharedTestsRunner.Run(path);");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Files.write(TEST_OUTPUT_DIRECTORY.resolve(pascalGroupName + "Tests" + ".java"), writer.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static Test readTest(File file) throws IOException {
        var reader = new BufferedReader(new FileReader(file));
        reader.readLine();
        return mapper.readValue("{" + reader.readLine().replaceAll(".$", "") + "}", Test.class);
    }

    public static class TestGroup {
        public String protocolVersion;
        public String name;

        public TestGroup(){

        }
    }

    public static class Test {
        public String name;

        public Test(){

        }
    }
}
