package com.edgedb.driver.util;

import java.lang.ref.Cleaner;

public class CleanerProvider {
    private static final Cleaner CLEANER = Cleaner.create();

    public static Cleaner getCleaner() {
        return CLEANER;
    }

}
