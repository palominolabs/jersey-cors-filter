package com.palominolabs.jersey.cors;

/**
 * Jersey property names to use to configure what corresponding HTTP headers should be included in responses.
 */
final class CorsConfig {
    private static final String PREFIX = "com.palominolabs.jersey.cors.";

    /**
     * Expiration of preflight response caching in seconds as either a java.lang.Integer or a java.lang.String. The
     * default is 24 * 3600 (1 day).
     */
    public static final String MAX_AGE = PREFIX + "maxAage";
    /**
     * Comma-separated list of HTTP methods. The default is "GET". The header is only sent if this is non-empty.
     */
    public static final String ALLOW_METHODS = PREFIX + "allowMethods";
    /**
     * Comma-separated list of HTTP headers. The default is "". The header is only sent if this is non-empty.
     */
    public static final String ALLOW_HEADERS = PREFIX + "allowHeaders";
    /**
     * Either java.lang.Boolean or string 'true' or 'false'. The default is false. The header is only sent if this is
     * true.
     */
    public static final String ALLOW_CREDENTIALS = PREFIX + "allowCredentials";
    /**
     * String '*', 'null' (as a string), or an origin URI. The default is '*'.
     */
    public static final String ALLOW_ORIGIN = PREFIX + "allowOrigin";
    /**
     * Comma-separated list of HTTP headers. The default is "". The header is only sent if this is non-empty.
     */
    public static final String EXPOSE_HEADERS = PREFIX + "exposeHeaders";
}
