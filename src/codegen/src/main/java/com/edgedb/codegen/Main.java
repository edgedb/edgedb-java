package com.edgedb.codegen;

import com.edgedb.codegen.commands.Generate;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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

    public static void main(String[] args) {
        if(args.length == 0) {
            displayHelp();
            System.exit(0);
        }

        var command = COMMANDS.get(args[0]);

        if(command == null) {
            displayUnknownCommand(args[0]);
            System.exit(1);
        }

        if(args.length >= 2 && args[1].equalsIgnoreCase("help")) {
            new HelpFormatter().printHelp(args[0], command.get().getCommandOptions());
            System.exit(2);
        }

        var commandInstance = command.get();

        try {
            commandInstance.execute(new DefaultParser().parse(commandInstance.getCommandOptions(), args))
                    .toCompletableFuture()
                    .get();
        } catch (InterruptedException | EdgeDBException | IOException | ParseException | ExecutionException e) {
            System.err.println("Failed to run generator");
            System.err.println(e);
            System.exit(3);
        }

        System.exit(0);
    }

    private static void displayUnknownCommand(String command) {
        System.out.println("Unknown command \"" + command + "\"");
        displayHelp();
    }

    private static void displayHelp() {
        var formatter = new HelpFormatter();
        System.out.println("EdgeDB Java Code generator"); // TODO: print version
        for (var cmd : COMMANDS.entrySet()) {
            var commandInstance = cmd.getValue().get();
            formatter.printHelp(commandInstance.getSyntax(), commandInstance.getCommandOptions());
        }
    }
}