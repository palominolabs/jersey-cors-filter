package com.palominolabs.jersey.cors;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
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
@Provider
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
        if (method.isAnnotationPresent(Cors.class)) {
            if (method.isAnnotationPresent(OPTIONS.class)) {
                logger.error("Resource method " + abstractMethod +
                    " is annotated with @Cors, which is not applicable for methods annotated with @OPTIONS");
                return null;
            }

            return newArrayList(getResourceResponseFilter(method.getAnnotation(Cors.class)));
        }

        if (method.isAnnotationPresent(CorsPreflight.class)) {
            if (!method.isAnnotationPresent(OPTIONS.class)) {
                logger.error("Resource method " + abstractMethod +
                    " is annotated with @CorsPreflight, which is only applicable for methods annotated with @OPTIONS");
                return null;
            }

            return newArrayList(getPreflightResponseFilter(method.getAnnotation(CorsPreflight.class)));
        }

        return null;
    }

    private ResourceFilter getResourceResponseFilter(Cors cors) {
        String allowOrigin = cors.allowOrigin().isEmpty() ? defAllowOrigin : cors.allowOrigin();
        String exposeHeaders = cors.exposeHeaders().isEmpty() ? defExposeHeaders : cors.exposeHeaders();
        Ternary allowCredentials =
            cors.allowCredentials() == NEUTRAL ? defAllowCredentials : cors.allowCredentials();

        return new CorsResourceResponseResourceFilter(allowOrigin, exposeHeaders,
            getBooleanFromTernary(allowCredentials));
    }

    private ResourceFilter getPreflightResponseFilter(CorsPreflight cors) {
        int maxAge = cors.maxAge() == -1 ? defMaxAge : cors.maxAge();
        String allowMethods = cors.allowMethods().isEmpty() ? defAllowMethods : cors.allowMethods();
        String allowHeaders = cors.allowHeaders().isEmpty() ? defAllowHeaders : cors.allowHeaders();
        Ternary allowCredentials = cors.allowCredentials() == NEUTRAL ? defAllowCredentials : cors.allowCredentials();

        return new CorsPreflightResponseResourceFilter(maxAge, allowMethods, allowHeaders,
            getBooleanFromTernary(allowCredentials));
    }

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
}
