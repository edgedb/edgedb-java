package com.edgedb.codegen;

import com.edgedb.driver.exceptions.EdgeDBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public interface Command {
    Options getCommandOptions();

    CompletionStage<Void> execute(CommandLine commandLine) throws EdgeDBException, IOException;
}
