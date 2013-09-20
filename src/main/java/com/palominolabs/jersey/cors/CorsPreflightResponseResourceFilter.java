package com.palominolabs.jersey.cors;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.annotation.concurrent.Immutable;

@Immutable
final class CorsPreflightResponseResourceFilter implements ResourceFilter {

    private final ContainerResponseFilter responseFilter;

    CorsPreflightResponseResourceFilter(int maxAge, String allowMethods, String allowHeaders,
        boolean allowCredentials) {

        responseFilter = new CorsPreflightContainerResponseFilter(maxAge, allowMethods, allowHeaders, allowCredentials);
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

        private CorsPreflightContainerResponseFilter(int maxAge, String allowMethods, String allowHeaders,
            boolean allowCredentials) {
            this.maxAge = maxAge;
            this.allowMethods = allowMethods;
            this.allowHeaders = allowHeaders;
            this.allowCredentials = allowCredentials;
        }

        @Override
        public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
            return null;// TODO
        }
    }
}
