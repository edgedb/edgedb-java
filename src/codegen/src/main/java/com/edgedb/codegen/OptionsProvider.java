package com.edgedb.codegen;

import org.apache.commons.cli.Option;

public interface OptionsProvider {
    Option[] getOptions();
}
