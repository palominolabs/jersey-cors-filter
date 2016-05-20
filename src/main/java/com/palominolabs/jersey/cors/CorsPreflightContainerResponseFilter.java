package com.palominolabs.jersey.cors;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

import static com.palominolabs.jersey.cors.CorsResponseContainerResponseFilter.putIfNotPresent;

@Immutable
final class CorsPreflightContainerResponseFilter implements ContainerResponseFilter {
    private final int maxAge;

    private final String allowMethods;

    private final String allowHeaders;

    private final boolean allowCredentials;

    private final String allowOrigin;

    CorsPreflightContainerResponseFilter(int maxAge, String allowMethods, String allowHeaders,
                                         boolean allowCredentials, String allowOrigin) {
        this.maxAge = maxAge;
        this.allowMethods = allowMethods;
        this.allowHeaders = allowHeaders;
        this.allowCredentials = allowCredentials;
        this.allowOrigin = allowOrigin;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext responseContext) throws IOException {
        String incomingOrigin = request.getHeaderString(CorsHeaders.ORIGIN);
        if (incomingOrigin == null) {
            return;
        }

        MultivaluedMap<String, Object> h = responseContext.getHeaders();
        putIfNotPresent(h, CorsHeaders.ALLOW_ORIGIN, allowOrigin);
        putIfNotPresent(h, CorsHeaders.MAX_AGE, Integer.toString(maxAge));
        if (!allowMethods.isEmpty()) {
            putIfNotPresent(h, CorsHeaders.ALLOW_METHODS, allowMethods);
        }
        if (!allowHeaders.isEmpty()) {
            putIfNotPresent(h, CorsHeaders.ALLOW_HEADERS, allowHeaders);
        }
        if (allowCredentials) {
            putIfNotPresent(h, CorsHeaders.ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
        }
    }
}
