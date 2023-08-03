package com.edgedb.codegen;

import com.edgedb.codegen.commands.Generate;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class Main {
    private static final Map<String, Supplier<Command>> COMMANDS = new HashMap<>() {{
        put("generate", Generate::new);
    }};

    public static void main(String[] args) throws ParseException, ExecutionException, InterruptedException, EdgeDBException, IOException {
        if(args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        var command = COMMANDS.get(args[0]);

        if(command == null) {
            displayUnknownCommand();
            System.exit(0);
        }

        var commandInstance = command.get();

        commandInstance.execute(new DefaultParser().parse(commandInstance.getCommandOptions(), args))
                .toCompletableFuture()
                .get();
    }

    private static void displayUnknownCommand(){

    }

    private static void displayHelp() {

    }
}