package com.gel.driver.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks the current target ignored for the binding.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface  GelIgnore {
}
