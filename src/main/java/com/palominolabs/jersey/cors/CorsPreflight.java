package com.palominolabs.jersey.cors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.palominolabs.jersey.cors.Ternary.NEUTRAL;

/**
 * Controls the CORS headers used on preflight responses. Since this only applies to preflight requests, it is only
 * applicable to methods annotated with {@link javax.ws.rs.OPTIONS}
 *
 * The elements of the annotation are all optional, and only need to be specified if the the global configuration should
 * be overridden for a particular annotated method.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorsPreflight {

    /**
     * @return how many seconds the result of a preflight request can be cached by the user agent
     */
    int maxAge() default -1;

    /**
     * @return comma-separated list of allowed HTTP methods
     */
    String allowMethods() default "";

    /**
     * @return comma-separated list of allowed HTTP headers
     */
    String allowHeaders() default "";

    /**
     * @return true if request that is being preflighted should be allowed to be made with the 'credentials' flag
     */
    Ternary allowCredentials() default NEUTRAL;
}
