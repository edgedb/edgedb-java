package com.edgedb.testgen;

import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final String TEST_DEFINITION_DIRECTORY = "C:\\Users\\lynch\\source\\repos\\EdgeDB\\tests\\EdgeDB.Tests.Integration\\tests";
    private static final String TEST_OUTPUT_DIRECTORY = "C:\\Users\\lynch\\Documents\\GitHub\\edgedb-java\\src\\driver\\src\\test\\java\\shared\\generated";

    private static final NamingStrategy PASCAL_CASE_NAMING_STRATEGY = NamingStrategy.pascalCase();
    private static final NamingStrategy CAMEL_CASE_NAMING_STRATEGY = NamingStrategy.camelCase();

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public static void main(String[] args) throws IOException {
        var definitionDir = new File(TEST_DEFINITION_DIRECTORY);

        var files = definitionDir.listFiles((d,e) -> d.getAbsolutePath() == definitionDir.getAbsolutePath() && e.endsWith(".json"));

        if(files == null) {
            throw new FileNotFoundException("No files in definition directory");
        }

        for(var file : files) {
            var group = mapper.readValue(Files.readString(file.toPath(), StandardCharsets.UTF_8), TestGroup.class);
            var testFiles = Path.of(TEST_DEFINITION_DIRECTORY, file.getName().replace(".json", "")).toFile().listFiles();
            processGroup(group, testFiles);
        }
    }

    private static final String[] usings = new String[] {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.DisplayName",
            "shared.SharedTestsRunner"
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
                    writer.appendLine("var path = \"" + testFile.getAbsolutePath().replace("\\", "\\\\") + "\";");
                    writer.appendLine("SharedTestsRunner.Run(path);");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Files.write(Path.of(TEST_OUTPUT_DIRECTORY, pascalGroupName + "Tests" + ".java"), writer.toString().getBytes(StandardCharsets.UTF_8));
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
