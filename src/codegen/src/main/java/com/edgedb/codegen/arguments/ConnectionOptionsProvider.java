package com.edgedb.codegen.arguments;

import com.edgedb.codegen.OptionsProvider;
import com.edgedb.driver.EdgeDBConnection;
import com.edgedb.driver.TLSSecurityMode;
import com.edgedb.driver.exceptions.ConfigurationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public interface ConnectionOptionsProvider extends OptionsProvider {
    JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    Option[] CONNECTION_OPTIONS = new Option[] {
            Option.builder()
                    .longOpt("dsn")
                    .desc("DSN for EdgeDB to connect to (overrides all other options except password)")
                    .hasArg()
                    .build(),
            Option.builder()
                    .longOpt("credentials-file")
                    .desc("Path to JSON file to read credentials from")
                    .hasArg()
                    .build(),
            Option.builder()
                    .option("I")
                    .longOpt("instance")
                    .desc("Local instance name created with edgedb instance create to connect to (overrides host and port)")
                    .hasArg()
                    .build(),
            Option.builder()
                    .option("H")
                    .longOpt("host")
                    .desc("Host of the EdgeDB instance")
                    .hasArg()
                    .build(),
            Option.builder()
                    .option("P")
                    .longOpt("port")
                    .desc("Port to connect to EdgeDB")
                    .hasArg()
                    .build(),
            Option.builder()
                    .option("d")
                    .longOpt("database")
                    .desc("Database name to connect to")
                    .hasArg()
                    .build(),
            Option.builder()
                    .option("u")
                    .longOpt("user")
                    .desc("User name of the EdgeDB user")
                    .hasArg()
                    .build(),
            Option.builder()
                    .longOpt("password")
                    .desc("Ask for password on the terminal (TTY)")
                    .build(),
            Option.builder()
                    .longOpt("password-from-stdin")
                    .desc("Read the password from stdin rather than TTY (useful for scripts)")
                    .build(),
            Option.builder()
                    .longOpt("tls-ca-file")
                    .desc("Certificate to match server against\n\nThis might either be full self-signed server certificate or certificate authority (CA) certificate that server certificate is signed with")
                    .hasArg()
                    .build(),
            Option.builder()
                    .longOpt("tls-security")
                    .desc("Specify the client-side TLS security mode")
                    .hasArg()
                    .build(),
    };

    @Override
    default Option[] getOptions() {
        return CONNECTION_OPTIONS;
    }

    default EdgeDBConnection getConnection(CommandLine cmd) throws ConfigurationException, IOException {
        if(cmd.hasOption("dsn")) {
            return EdgeDBConnection.fromDSN(cmd.getOptionValue("dsn"));
        }

        if(cmd.hasOption("instance")) {
            return EdgeDBConnection.fromInstanceName(cmd.getOptionValue("instance"));
        }

        if(cmd.hasOption("credentials-file")) {
            var credentialsFile = Path.of(cmd.getOptionValue("credentials-file"));

            if(!Files.exists(credentialsFile)) {
                throw new FileNotFoundException("Could not find the file defined for the option 'credentials-file'");
            }

            return JSON_MAPPER.readValue(Files.readString(credentialsFile, StandardCharsets.UTF_8), EdgeDBConnection.class);
        }

        return EdgeDBConnection.parse(builder -> {
           if(cmd.hasOption("host")) {
               builder.withHostname(cmd.getOptionValue("host"));
           }

           if(cmd.hasOption("port")) {
               try {
                   builder.withPort(Integer.parseInt(cmd.getOptionValue("port")));
               } catch (NumberFormatException x) {
                   throw new ConfigurationException("The format of 'port' is not a valid integer", x);
               }
           }

           if(cmd.hasOption("database")) {
               builder.withDatabase(cmd.getOptionValue("database"));
           }

           if(cmd.hasOption("user")) {
               builder.withUser(cmd.getOptionValue("user"));
           }

           if(cmd.hasOption("password")) {
               System.out.print("Password: ");
               try(var scanner = new Scanner(System.in)) {
                   builder.withPassword(scanner.nextLine());
               }
           }

           if(cmd.hasOption("password-from-stdin")) {
               try(var scanner = new Scanner(System.in)) {
                   builder.withPassword(scanner.nextLine());
               }
           }

           if(cmd.hasOption("tls-ca-file")) {
               var tlscsPath = Path.of(cmd.getOptionValue("tls-ca-file"));

               if(!Files.exists(tlscsPath)) {
                   throw new ConfigurationException("Could not find the file defined for the option 'tls-ca-file'");
               }

               try {
                   builder.withTlsca(Files.readString(tlscsPath));
               } catch (IOException e) {
                   throw new ConfigurationException("Failed to read 'tls-ca-file'", e);
               }
           }

           if(cmd.hasOption("tls-security")) {
               builder.withTlsSecurity(Enum.valueOf(TLSSecurityMode.class, cmd.getOptionValue("tls-security")));
           }
        });
    }
}
