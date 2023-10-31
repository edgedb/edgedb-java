package com.edgedb.codegen.arguments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.edgedb.codegen.OptionsProvider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.slf4j.LoggerFactory;

public interface LoggerOptionsProvider extends OptionsProvider {
    Option[] LOGGING_OPTIONS = new Option[] {
            Option.builder()
                    .hasArg()
                    .longOpt("log-level")
                    .desc("Sets the log level")
                    .argName("level")
                    .build()
    };

    @Override
    default Option[] getOptions() {
        return LOGGING_OPTIONS;
    }

    default Logger getLogger(Class<?> cls, CommandLine cl) {
        var logger = (Logger) LoggerFactory.getLogger(cls);

        var cliValue = cl.getOptionValue("log-level");
        logger.setLevel(Level.toLevel(cliValue, Level.INFO));

        return logger;
    }
}
