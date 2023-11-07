package com.edgedb.codegen.commands;

import com.edgedb.codegen.Command;
import com.edgedb.codegen.arguments.ConnectionOptionsProvider;
import com.edgedb.codegen.arguments.LoggerOptionsProvider;
import com.edgedb.codegen.generator.CodeGenerator;
import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.utils.PathUtils;
import com.edgedb.codegen.utils.ProjectUtils;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Generate implements Command, ConnectionOptionsProvider, LoggerOptionsProvider {
    @Override
    public String getSyntax() {
        return "generate <options> <connection>";
    }

    @Override
    public Options getCommandOptions() {
        var options = new Options();
        for (var option : getOptions()) {
            options.addOption(option);
        }

        return options
                .addOption("o", "output", true, "The output directory for the generated " +
                        "source to be placed. When generating a project, source files will be placed in that projects " +
                        "directory. Default is the current directory")
                .addOption("n", "package-name", true, "The package name for generated files.")
                .addOption("f", "force", false, "Force regeneration of all query files.");
    }

    @Override
    public CompletionStage<Void> execute(CommandLine commandLine) throws EdgeDBException, IOException {
        var connection = getConnection(commandLine);
        var logger = getLogger(Generate.class, commandLine);

        var projectRoot = ProjectUtils.getProjectRoot();
        var outputDirectory = PathUtils.fromRelativeInput(commandLine.getOptionValue("output", System.getProperty("user.dir")));

        if(!Files.exists(outputDirectory)) {
            Files.createDirectory(outputDirectory);
        }

        var edgeqlFiles = ProjectUtils.getTargetEdgeQLFiles(projectRoot);

        var matching = edgeqlFiles.stream()
                .map(a ->
                        Map.entry(a, edgeqlFiles.stream()
                                .filter(b ->
                                        Path.of(a).getFileName().equals(Path.of(b).getFileName()
                                        )
                                )
                                .collect(Collectors.toList()))
                )
                .filter(a -> a.getValue().size() > 1)
                .collect(Collectors.toList());

        for (var match : matching) {
            logger.error(
                    "Duplicate files detected: {} has {} other files with the same name ({})",
                    match.getKey(), match.getValue().size(), String.join(", ", match.getValue())
            );
        }

        if(!matching.isEmpty()) {
            return CompletableFuture.failedFuture(new EdgeDBException("Duplicate file names found"));
        }

        var generator = new CodeGenerator(connection, getLogger(CodeGenerator.class, commandLine));
        var context = new GeneratorContext(
                outputDirectory,
                commandLine.getOptionValue("package-name", "com.edgedb.generated"),
                generator
        );

        return generator.generate(edgeqlFiles, context, null, commandLine.hasOption("force"));
    }

    @Override
    public Option[] getOptions() {
        return Stream.concat(
                Arrays.stream(ConnectionOptionsProvider.super.getOptions()),
                Arrays.stream(LoggerOptionsProvider.super.getOptions())
        ).toArray(x -> (Option[]) Array.newInstance(Option.class, x));
    }
}
