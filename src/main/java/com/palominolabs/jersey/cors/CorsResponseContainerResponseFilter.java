package com.palominolabs.jersey.cors;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

@Immutable
final class CorsResponseContainerResponseFilter implements ContainerResponseFilter {
    private final String allowOrigin;

    private final String exposeHeaders;

    private final boolean allowCredentials;

    CorsResponseContainerResponseFilter(String allowOrigin, String exposeHeaders, boolean allowCredentials) {
        this.allowOrigin = allowOrigin;
        this.exposeHeaders = exposeHeaders;
        this.allowCredentials = allowCredentials;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String incomingOrigin = request.getHeaderString(CorsHeaders.ORIGIN);
        if (incomingOrigin == null) {
            return;
        }

        MultivaluedMap<String, Object> h = response.getHeaders();
        putIfNotPresent(h, CorsHeaders.ALLOW_ORIGIN, allowOrigin);
        if (!exposeHeaders.isEmpty()) {
            putIfNotPresent(h, CorsHeaders.EXPOSE_HEADERS, exposeHeaders);
        }
        if (allowCredentials) {
            putIfNotPresent(h, CorsHeaders.ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
        }
    }

    static void putIfNotPresent(MultivaluedMap<String, Object> h, String header, String value) {
        if (!h.containsKey(header)) {
            h.putSingle(header, value);
        }
    }
}
