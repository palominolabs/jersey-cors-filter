package com.palominolabs.jersey.cors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import java.util.Map;

import static com.palominolabs.jersey.cors.CorsConfig.*;
import static com.palominolabs.jersey.cors.CorsPreflight.UNSET_MAX_AGE;
import static com.palominolabs.jersey.cors.Ternary.*;

/**
 * Jersey ResourceFilterFactory that applies filters to set appropriate headers regular resource methods (@GET, @POST)
 * annotated with {@link Cors} as well as preflight @OPTIONS methods annotated with {@link CorsPreflight}.
 *
 * The default settings allow all origins to use GET and instruct the user agent to cache that for 1 day, but do not
 * allow any other headers, methods, or credentials. You can override these defaults by specifying the properties found
 * in {@link CorsConfig} as Jersey params.
 */
@Immutable
public final class CorsResourceFilter implements DynamicFeature {
    private static final Logger logger = LoggerFactory.getLogger(CorsResourceFilter.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> klass = method.getDeclaringClass();

        // check for impossible combinations
        if (method.isAnnotationPresent(OPTIONS.class) && method.isAnnotationPresent(Cors.class)) {
            logger.error("Resource method " + resourceInfo +
                    " is annotated with @Cors, which is not applicable for methods annotated with @OPTIONS");
            return;
        } else if (!method.isAnnotationPresent(OPTIONS.class) && method.isAnnotationPresent(CorsPreflight.class)) {
            logger.error("Resource method " + resourceInfo +
                    " is annotated with @CorsPreflight, which is only applicable for methods annotated with @OPTIONS");
            return;
        }

        addCorsFilter(method, klass, context);

        addCorsPreflightFilter(method, klass, context);
    }

    /**
     * Add Cors response filter, if appropriate.
     *
     * @param method  method
     * @param klass   method's class
     * @param context resource's context
     */
    private void addCorsFilter(Method method, Class<?> klass, FeatureContext context) {
        if (!klass.isAnnotationPresent(Cors.class) && !method.isAnnotationPresent(Cors.class)) {
            return;
        }

        CorsResourceConfig config = getDefaultResourceConfig(context);

        if (klass.isAnnotationPresent(Cors.class)) {
            if (method.isAnnotationPresent(OPTIONS.class)) {
                // do not add a filter
                return;
            }
            applyCorsAnnotation(config, klass.getAnnotation(Cors.class));
        }

        if (method.isAnnotationPresent(Cors.class)) {
            applyCorsAnnotation(config, method.getAnnotation(Cors.class));
        }

        context.register(getResourceResponseFilter(config));
    }

    /**
     * Add CorsPreflight response filter, if appropriate.
     *
     * @param method  method
     * @param klass   method's class
     * @param context resource's context
     */
    private void addCorsPreflightFilter(Method method, Class<?> klass, FeatureContext context) {
        if (!klass.isAnnotationPresent(CorsPreflight.class) && !method.isAnnotationPresent(CorsPreflight.class)) {
            return;
        }

        CorsPreflightConfig config = getDefaultPreflightConfig(context);

        if (klass.isAnnotationPresent(CorsPreflight.class)) {
            if (!method.isAnnotationPresent(OPTIONS.class)) {
                return;
            }

            applyCorsPreflightAnnotation(config, klass.getAnnotation(CorsPreflight.class));
        }

        if (method.isAnnotationPresent(CorsPreflight.class)) {
            applyCorsPreflightAnnotation(config, method.getAnnotation(CorsPreflight.class));
        }

        context.register(getPreflightResponseFilter(config));
    }

    private CorsResponseContainerResponseFilter getResourceResponseFilter(CorsResourceConfig config) {
        return new CorsResponseContainerResponseFilter(config.allowOrigin, config.exposeHeaders,
            getBooleanFromTernary(config.allowCredentials));
    }

    private CorsPreflightContainerResponseFilter getPreflightResponseFilter(CorsPreflightConfig config) {
        return new CorsPreflightContainerResponseFilter(config.maxAge, config.allowMethods, config.allowHeaders,
            getBooleanFromTernary(config.allowCredentials), config.allowOrigin);
    }

    /**
     * Write non-default values in the annotation to the config.
     *
     * @param config config to write to
     * @param ann    annotation to read from
     */
    private static void applyCorsAnnotation(CorsResourceConfig config, Cors ann) {
        if (!ann.allowOrigin().isEmpty()) {
            config.allowOrigin = ann.allowOrigin();
        }

        if (!ann.exposeHeaders().isEmpty()) {
            config.exposeHeaders = ann.exposeHeaders();
        }

        if (ann.allowCredentials() != NEUTRAL) {
            config.allowCredentials = ann.allowCredentials();
        }
    }

    /**
     * Write non-default values in the annotation to the config.
     *
     * @param config config to write to
     * @param ann    annotation to read from
     */
    private static void applyCorsPreflightAnnotation(CorsPreflightConfig config, CorsPreflight ann) {
        if (ann.maxAge() != UNSET_MAX_AGE) {
            config.maxAge = ann.maxAge();
        }

        if (!ann.allowMethods().isEmpty()) {
            config.allowMethods = ann.allowMethods();
        }

        if (!ann.allowHeaders().isEmpty()) {
            config.allowHeaders = ann.allowHeaders();
        }

        if (ann.allowCredentials() != NEUTRAL) {
            config.allowCredentials = ann.allowCredentials();
        }

        if (!ann.allowOrigin().isEmpty()) {
            config.allowOrigin = ann.allowOrigin();
        }
    }

    /**
     * @return a CorsResourceConfig filled in based on the current defaults.
     */
    @Nonnull
    private CorsResourceConfig getDefaultResourceConfig(FeatureContext context) {
        CorsResourceConfig c = new CorsResourceConfig();
        Map<String, Object> properties = context.getConfiguration().getProperties();

        c.allowOrigin = getStringProp(properties, ALLOW_ORIGIN, "*");
        c.exposeHeaders = getStringProp(properties, EXPOSE_HEADERS, "");
        c.allowCredentials = getBooleanProp(properties, ALLOW_CREDENTIALS, false) ? TRUE : FALSE;

        return c;
    }

    /**
     * @return a CorsPreflightConfig filled in based on the current defaults.
     */
    @Nonnull
    private CorsPreflightConfig getDefaultPreflightConfig(FeatureContext context) {
        CorsPreflightConfig c = new CorsPreflightConfig();
        Map<String, Object> properties = context.getConfiguration().getProperties();

        c.maxAge = getIntProp(properties, MAX_AGE, 24 * 3600);
        c.allowMethods = getStringProp(properties, ALLOW_METHODS, "GET");
        c.allowHeaders = getStringProp(properties, ALLOW_HEADERS, "");
        c.allowCredentials = getBooleanProp(properties, ALLOW_CREDENTIALS, false) ? TRUE : FALSE;
        c.allowOrigin = getStringProp(properties, ALLOW_ORIGIN, "*");

        return c;
    }

    /**
     * @param allowCredentials a Ternary
     * @return true for TRUE, false for FALSE
     * @throws IllegalStateException on NEUTRAL
     */
    private static boolean getBooleanFromTernary(Ternary allowCredentials) {
        switch (allowCredentials) {
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NEUTRAL:
            default: // default unnecessary but helps javac understand that all cases are covered
                throw new IllegalStateException("Neutral ternary; impossible");
        }
    }

    private static String getStringProp(Map<String, Object> props, String propName, String defaultValue) {
        return props.containsKey(propName) ? getString(props.get(propName)) : defaultValue;
    }

    private static int getIntProp(Map<String, Object> props, String propName, int defaultValue) {
        return props.containsKey(propName) ? getInt(props.get(propName)) : defaultValue;
    }

    private static boolean getBooleanProp(Map<String, Object> props, String propName, boolean defaultValue) {
        return props.containsKey(propName) ? getBoolean(props.get(propName)) : defaultValue;
    }

    @Nonnull
    private static String getString(Object propValue) {
        if (propValue instanceof String) {
            return (String) propValue;
        }

        throw new IllegalArgumentException("Could not parse " + propValue + " as a String");
    }

    private static int getInt(Object propValue) {
        if (propValue instanceof String) {
            return Integer.parseInt((String) propValue);
        } else if (propValue instanceof Integer) {
            return (Integer) propValue;
        }

        throw new IllegalArgumentException("Could not parse " + propValue + " as an int");
    }

    private static boolean getBoolean(Object propValue) {
        if (propValue instanceof String) {
            return Boolean.parseBoolean((String) propValue);
        } else if (propValue instanceof Boolean) {
            return (Boolean) propValue;
        }

        throw new IllegalArgumentException("Could not parse " + propValue + " as a bool");
    }

    /**
     * Mutable bundle of config data for resource cors headers.
     */
    @NotThreadSafe
    private static class CorsResourceConfig {
        String allowOrigin;
        String exposeHeaders;
        Ternary allowCredentials;
    }

    /**
     * Mutable bundle of config data for preflight cors headers.
     */
    @NotThreadSafe
    private static class CorsPreflightConfig {
        int maxAge;
        String allowMethods;
        String allowHeaders;
        Ternary allowCredentials;
        String allowOrigin;
    }
}
