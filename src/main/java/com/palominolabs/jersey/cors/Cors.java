package com.palominolabs.jersey.cors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controls the CORS headers used on resource responses for the annotated method. The elements of the annotation are all
 * optional, and only need to be specified if the the global configuration should be overridden for a particular
 * annotated method.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cors {

    /**
     * Note that "*" cannot be used if credentials are provided; you must use a specific origin.
     *
     * @return The string "*" or the URI that is an allowed origin
     */
    String allowOrigin() default "";

    /**
     * @return Comma-separated list of header names
     */
    String exposeHeaders() default "";

    /**
     * @return true if requests are allowed to be made with the 'credentials' flag, false otherwise. Neutral simply
     *         means use the global config (boxed Booleans are not allowed in annotations, hence the Ternary enum).
     */
    Ternary allowCredentials() default Ternary.NEUTRAL;
}
