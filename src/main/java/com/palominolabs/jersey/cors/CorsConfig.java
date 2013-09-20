package com.palominolabs.jersey.cors;

/**
 * Jersey property names to use to configure what corresponding HTTP headers should be included in responses.
 */
final class CorsConfig {
    private static final String PREFIX = "com.palominolabs.jersey.cors.";

    /**
     * Expiration of preflight response caching in seconds as either a java.lang.Integer or a java.lang.String.
     */
    public static final String MAX_AGE = PREFIX + "maxAage";
    /**
     * Comma-separated list of HTTP methods
     */
    public static final String ALLOW_METHODS = PREFIX + "allowMethods";
    /**
     * Comma-separated list of HTTP headers
     */
    public static final String ALLOW_HEADERS = PREFIX + "allowHeaders";
    /**
     * Boolean, either java.lang.Boolean or string 'true' or 'false'
     */
    public static final String ALLOW_CREDENTIALS = PREFIX + "allowCredentials";
    /**
     * String '*' or an origin URI
     */
    public static final String ALLOW_ORIGIN = PREFIX + "allowOrigin";
    /**
     * Comma-separated list of HTTP headers
     */
    public static final String EXPOSE_HEADERS = PREFIX + "exposeHeaders";
}
