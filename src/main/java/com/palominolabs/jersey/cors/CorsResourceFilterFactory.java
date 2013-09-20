package com.palominolabs.jersey.cors;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.OPTIONS;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.palominolabs.jersey.cors.Ternary.FALSE;
import static com.palominolabs.jersey.cors.Ternary.NEUTRAL;
import static com.palominolabs.jersey.cors.Ternary.TRUE;

@Immutable
public final class CorsResourceFilterFactory implements ResourceFilterFactory {

    public static final String MAX_AGE = "palominolabs.jersey.cors.maxAage";
    public static final String ALLOW_METHODS = "palominolabs.jersey.cors.allowMethods";
    public static final String ALLOW_HEADERS = "palominolabs.jersey.cors.allowHeaders";
    public static final String ALLOW_CREDENTIALS = "palominolabs.jersey.cors.allowCredentials";
    public static final String ALLOW_ORIGIN = "palominolabs.jersey.cors.allowOrigin";
    public static final String EXPOSE_HEADERS = "palominolabs.jersey.cors.exposeHeaders";

    private static final Logger logger = LoggerFactory.getLogger(CorsResourceFilterFactory.class);

    private final int defMaxAge;
    private final String defAllowOrigin;
    private final String defExposeHeaders;
    private final Ternary defAllowCredentials;
    private final String defAllowMethods;
    private final String defAllowHeaders;

    public CorsResourceFilterFactory(ResourceConfig resourceConfig) {
        Map<String, Object> props = resourceConfig.getProperties();
        defAllowOrigin = getStringProp(props, ALLOW_ORIGIN, "*");
        defExposeHeaders = getStringProp(props, EXPOSE_HEADERS, "");
        defAllowCredentials = getBooleanProp(props, ALLOW_CREDENTIALS, false) ? TRUE : FALSE;
        defMaxAge = getIntProp(props, MAX_AGE, 24 * 3600);
        defAllowMethods = getStringProp(props, ALLOW_METHODS, "");
        defAllowHeaders = getStringProp(props, ALLOW_HEADERS, "");
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod abstractMethod) {
        if (!(abstractMethod instanceof AbstractResourceMethod)) {
            return null;
        }

        Method method = abstractMethod.getMethod();
        if (method.isAnnotationPresent(Cors.class)) {
            // apply filter
            return newArrayList(getResourceResponseFilter(method.getAnnotation(Cors.class)));
        }

        if (method.isAnnotationPresent(CorsPreflight.class)) {
            if (!method.isAnnotationPresent(OPTIONS.class)) {
                logger.error("Resource method " + abstractMethod +
                    " is annotated with CorsPreflight, which is only applicable when also annotated with OPTIONS");
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
