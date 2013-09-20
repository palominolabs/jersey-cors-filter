package com.palominolabs.jersey.cors;

final class CorsConfig {
    private static final String PREFIX = "com.palominolabs.jersey.cors.";

    public static final String MAX_AGE = PREFIX + "maxAage";
    public static final String ALLOW_METHODS = PREFIX + "allowMethods";
    public static final String ALLOW_HEADERS = PREFIX + "allowHeaders";
    public static final String ALLOW_CREDENTIALS = PREFIX + "allowCredentials";
    public static final String ALLOW_ORIGIN = PREFIX + "allowOrigin";
    public static final String EXPOSE_HEADERS = PREFIX + "exposeHeaders";
}
