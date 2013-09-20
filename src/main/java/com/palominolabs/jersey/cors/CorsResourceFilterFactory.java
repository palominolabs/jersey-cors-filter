package com.palominolabs.jersey.cors;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.palominolabs.jersey.cors.CorsConfig.ALLOW_CREDENTIALS;
import static com.palominolabs.jersey.cors.CorsConfig.ALLOW_HEADERS;
import static com.palominolabs.jersey.cors.CorsConfig.ALLOW_METHODS;
import static com.palominolabs.jersey.cors.CorsConfig.ALLOW_ORIGIN;
import static com.palominolabs.jersey.cors.CorsConfig.EXPOSE_HEADERS;
import static com.palominolabs.jersey.cors.CorsConfig.MAX_AGE;
import static com.palominolabs.jersey.cors.CorsPreflight.UNSET_MAX_AGE;
import static com.palominolabs.jersey.cors.Ternary.FALSE;
import static com.palominolabs.jersey.cors.Ternary.NEUTRAL;
import static com.palominolabs.jersey.cors.Ternary.TRUE;

/**
 * Jersey ResourceFilterFactory that applies filters to set appropriate headers regular resource methods (@GET, @POST)
 * annotated with {@link Cors} as well as preflight @OPTIONS methods annotated with {@link CorsPreflight}.
 *
 * The default settings allow all origins to use GET and instruct the user agent to cache that for 1 day, but do not
 * allow any other headers, methods, or credentials. You can override these defaults by specifying the properties found
 * in {@link CorsConfig} as Jersey params.
 */
@Immutable
public final class CorsResourceFilterFactory implements ResourceFilterFactory {

    private static final Logger logger = LoggerFactory.getLogger(CorsResourceFilterFactory.class);

    @VisibleForTesting
    final int defMaxAge;
    @VisibleForTesting
    final String defAllowOrigin;
    @VisibleForTesting
    final String defExposeHeaders;
    @VisibleForTesting
    final Ternary defAllowCredentials;
    @VisibleForTesting
    final String defAllowMethods;
    @VisibleForTesting
    final String defAllowHeaders;

    public CorsResourceFilterFactory(@Context ResourceConfig resourceConfig) {
        Map<String, Object> props = resourceConfig.getProperties();
        // load properties, if they are set, otherwise use hardcoded defaults.
        defAllowOrigin = getStringProp(props, ALLOW_ORIGIN, "*");
        defExposeHeaders = getStringProp(props, EXPOSE_HEADERS, "");
        defAllowCredentials = getBooleanProp(props, ALLOW_CREDENTIALS, false) ? TRUE : FALSE;
        defMaxAge = getIntProp(props, MAX_AGE, 24 * 3600);
        defAllowMethods = getStringProp(props, ALLOW_METHODS, "GET");
        defAllowHeaders = getStringProp(props, ALLOW_HEADERS, "");
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod abstractMethod) {
        if (!(abstractMethod instanceof AbstractResourceMethod)) {
            return null;
        }

        Method method = abstractMethod.getMethod();
        Class<?> klass = method.getDeclaringClass();
        List<ResourceFilter> filters = newArrayList();

        // check for impossible combinations
        if (method.isAnnotationPresent(OPTIONS.class) && method.isAnnotationPresent(Cors.class)) {
            logger.error("Resource method " + abstractMethod +
                " is annotated with @Cors, which is not applicable for methods annotated with @OPTIONS");
            return null;
        } else if (!method.isAnnotationPresent(OPTIONS.class) && method.isAnnotationPresent(CorsPreflight.class)) {
            logger.error("Resource method " + abstractMethod +
                " is annotated with @CorsPreflight, which is only applicable for methods annotated with @OPTIONS");
            return null;
        }

        addCorsFilter(method, klass, filters);

        addCorsPreflightFilter(method, klass, filters);

        return filters;
    }

    /**
     * Add Cors response filter, if appropriate.
     *
     * @param method  method
     * @param klass   method's class
     * @param filters filter list to add to
     */
    private void addCorsFilter(Method method, Class<?> klass, List<ResourceFilter> filters) {
        if (!klass.isAnnotationPresent(Cors.class) && !method.isAnnotationPresent(Cors.class)) {
            return;
        }

        CorsResourceConfig config = getDefaultResourceConfig();

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

        filters.add(getResourceResponseFilter(config));
    }

    /**
     * Add CorsPreflight response filter, if appropriate.
     *
     * @param method  method
     * @param klass   method's class
     * @param filters filter list to add to
     */
    private void addCorsPreflightFilter(Method method, Class<?> klass, List<ResourceFilter> filters) {
        if (!klass.isAnnotationPresent(CorsPreflight.class) && !method.isAnnotationPresent(CorsPreflight.class)) {
            return;
        }

        CorsPreflightConfig config = getDefaultPreflightConfig();

        if (klass.isAnnotationPresent(CorsPreflight.class)) {
            if (!method.isAnnotationPresent(OPTIONS.class)) {
                return;
            }

            applyCorsPreflightAnnotation(config, klass.getAnnotation(CorsPreflight.class));
        }

        if (method.isAnnotationPresent(CorsPreflight.class)) {
            applyCorsPreflightAnnotation(config, method.getAnnotation(CorsPreflight.class));
        }

        filters.add(getPreflightResponseFilter(config));
    }

    private ResourceFilter getResourceResponseFilter(CorsResourceConfig config) {
        return new CorsResourceResponseResourceFilter(config.allowOrigin, config.exposeHeaders,
            getBooleanFromTernary(config.allowCredentials));
    }

    private ResourceFilter getPreflightResponseFilter(CorsPreflightConfig config) {
        return new CorsPreflightResponseResourceFilter(config.maxAge, config.allowMethods, config.allowHeaders,
            getBooleanFromTernary(config.allowCredentials));
    }

    /**
     * Write non-default values in the annotation to the config.
     *
     * @param config config to write to
     * @param ann    annotation to read from
     */
    private void applyCorsAnnotation(CorsResourceConfig config, Cors ann) {
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
    private void applyCorsPreflightAnnotation(CorsPreflightConfig config, CorsPreflight ann) {
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
    }

    /**
     * @return a CorsResourceConfig filled in based on the current defaults.
     */
    @Nonnull
    private CorsResourceConfig getDefaultResourceConfig() {
        CorsResourceConfig c = new CorsResourceConfig();
        c.allowOrigin = defAllowOrigin;
        c.exposeHeaders = defExposeHeaders;
        c.allowCredentials = defAllowCredentials;
        return c;
    }

    /**
     * @return a CorsPreflightConfig filled in based on the current defaults.
     */
    @Nonnull
    private CorsPreflightConfig getDefaultPreflightConfig() {
        CorsPreflightConfig c = new CorsPreflightConfig();
        c.maxAge = defMaxAge;
        c.allowMethods = defAllowMethods;
        c.allowHeaders = defAllowHeaders;
        c.allowCredentials = defAllowCredentials;
        return c;
    }

    /**
     * @param allowCredentials a Ternary
     * @return true for TRUE, false for FALSE
     * @throws IllegalStateException on NEUTRAL
     */
    private boolean getBooleanFromTernary(Ternary allowCredentials) {
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
    }
}
