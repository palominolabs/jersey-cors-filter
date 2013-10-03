package com.palominolabs.jersey.cors;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MultivaluedMap;

import static com.palominolabs.jersey.cors.CorsResourceResponseResourceFilter.putIfNotPresent;

@Immutable
final class CorsPreflightResponseResourceFilter implements ResourceFilter {

    private final ContainerResponseFilter responseFilter;

    CorsPreflightResponseResourceFilter(int maxAge, @Nonnull String allowMethods, @Nonnull String allowHeaders,
        boolean allowCredentials, String allowOrigin) {

        responseFilter = new CorsPreflightContainerResponseFilter(maxAge, allowMethods, allowHeaders, allowCredentials,
            allowOrigin);
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return null;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return responseFilter;
    }

    @Immutable
    private static class CorsPreflightContainerResponseFilter implements ContainerResponseFilter {

        private final int maxAge;

        private final String allowMethods;

        private final String allowHeaders;

        private final boolean allowCredentials;

        private final String allowOrigin;

        private CorsPreflightContainerResponseFilter(int maxAge, String allowMethods, String allowHeaders,
            boolean allowCredentials, String allowOrigin) {
            this.maxAge = maxAge;
            this.allowMethods = allowMethods;
            this.allowHeaders = allowHeaders;
            this.allowCredentials = allowCredentials;
            this.allowOrigin = allowOrigin;
        }

        @Override
        public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
            MultivaluedMap<String, Object> h = response.getHttpHeaders();
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
            return response;
        }
    }
}
