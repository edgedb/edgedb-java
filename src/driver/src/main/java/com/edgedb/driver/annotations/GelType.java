package com.edgedb.driver.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the target type as a type that is represented in EdgeDB.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GelType {
    /**
     * Gets or sets the module that this type resides in.
     * @return The module the type resides in if specified; otherwise {@code [UNASSIGNED]} is returned indicating that
     * the default or session-defined module should be assumed.
     */
    String module() default "[UNASSIGNED]";
}
