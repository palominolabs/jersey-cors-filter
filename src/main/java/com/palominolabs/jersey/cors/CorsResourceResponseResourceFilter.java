package com.palominolabs.jersey.cors;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.annotation.concurrent.Immutable;

@Immutable
final class CorsResourceResponseResourceFilter implements ResourceFilter {

    private final ContainerResponseFilter responseFilter;

    CorsResourceResponseResourceFilter(String allowOrigin, String exposeHeaders,
        boolean allowCredentials) {
        responseFilter = new CorsResponseContainerResponseFilter(allowOrigin, exposeHeaders, allowCredentials);
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
    private static class CorsResponseContainerResponseFilter implements ContainerResponseFilter {

        private final String allowOrigin;

        private final String exposeHeaders;

        private final boolean allowCredentials;

        private CorsResponseContainerResponseFilter(String allowOrigin, String exposeHeaders,
            boolean allowCredentials) {
            this.allowOrigin = allowOrigin;
            this.exposeHeaders = exposeHeaders;
            this.allowCredentials = allowCredentials;
        }

        @Override
        public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
            return null;// TODO
        }
    }
}
