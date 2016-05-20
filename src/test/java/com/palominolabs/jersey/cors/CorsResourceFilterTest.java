package com.palominolabs.jersey.cors;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static com.palominolabs.jersey.cors.CorsHeaders.*;
import static groovy.util.GroovyTestCase.assertEquals;
import static org.junit.Assert.assertFalse;

public class CorsResourceFilterTest extends JerseyTest {
    @Path("unannotated")
    public static class UnAnnotatedResource {
        @GET
        public String getWithCorsAnnotation() {
            return "foo";
        }
    }

    @Path("method-annotated")
    public static class MethodAnnotatedResource {
        @OPTIONS
        @CorsPreflight
        public void optionsWithCorsPreflightAnnotation() { }

        @GET
        @Cors
        public String getWithCorsAnnotation() {
            return "foo";
        }
    }

    @Path("class-annotated")
    @Cors
    @CorsPreflight
    public static class ClassAnnotatedResource {
        @OPTIONS
        public void optionsWithCorsPreflightAnnotation() { }

        @GET
        public String getWithCorsAnnotation() {
            return "foo";
        }
    }

    @Override
    protected Application configure() {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(CorsResourceFilter.class);
        resourceConfig.register(UnAnnotatedResource.class);
        resourceConfig.register(MethodAnnotatedResource.class);
        resourceConfig.register(ClassAnnotatedResource.class);

//        Map<String, Object> properties = new HashMap<>();
//        properties.put(CorsConfig.ALLOW_ORIGIN, "http://foo.com");
//        resourceConfig.setProperties(properties);

        return resourceConfig;
    }

    @Test
    public void testUnAnnotated() {
        Response response = target("unannotated").request()
                .header(ORIGIN, "foobar")
                .get();

        assertNoCorsHeadersResponse(response);
    }

    @Test
    public void testCorsPreflight_MethodAnnotated() {
        Response response = target("method-annotated").request()
                .header(ORIGIN, "foobar")
                .options();

        assertDefaultOptionsResponse(response);
    }

    @Test
    public void testCors_MethodAnnotated() {
        Response response = target("method-annotated").request()
                .header(ORIGIN, "foobar")
                .get();

        assertDefaultGetResponse(response);
    }

    @Test
    public void testCorsPreflight_ClassAnnotated() {
        Response response = target("class-annotated").request()
                .header(ORIGIN, "foobar")
                .options();

        assertDefaultOptionsResponse(response);
    }

    @Test
    public void testCors_ClassAnnotated() {
        Response response = target("class-annotated").request()
                .header(ORIGIN, "foobar")
                .get();

        assertDefaultGetResponse(response);
    }

    private static void assertNoCorsHeadersResponse(Response response) {
        Assert.assertEquals(200, response.getStatus());

        assertFalse(response.getHeaders().containsKey(ALLOW_ORIGIN));
        assertFalse(response.getHeaders().containsKey(ALLOW_CREDENTIALS));
        assertFalse(response.getHeaders().containsKey(EXPOSE_HEADERS));
        assertFalse(response.getHeaders().containsKey(MAX_AGE));
        assertFalse(response.getHeaders().containsKey(ALLOW_METHODS));
        assertFalse(response.getHeaders().containsKey(ALLOW_HEADERS));
    }

    private static void assertDefaultOptionsResponse(Response response) {
        Assert.assertEquals(204, response.getStatus());

        assertEquals("*", response.getHeaderString(ALLOW_ORIGIN));
        assertEquals("86400", response.getHeaderString(MAX_AGE));
        assertEquals("GET", response.getHeaderString(ALLOW_METHODS));
        assertEquals(null, response.getHeaderString(EXPOSE_HEADERS));
        assertEquals(null, response.getHeaderString(ALLOW_CREDENTIALS));
        assertEquals(null, response.getHeaderString(ALLOW_HEADERS));
    }

    private static void assertDefaultGetResponse(Response response) {
        Assert.assertEquals(200, response.getStatus());

        assertEquals("*", response.getHeaderString(ALLOW_ORIGIN));
        assertFalse(response.getHeaders().containsKey(ALLOW_CREDENTIALS));
        assertFalse(response.getHeaders().containsKey(EXPOSE_HEADERS));
        assertFalse(response.getHeaders().containsKey(MAX_AGE));
        assertFalse(response.getHeaders().containsKey(ALLOW_METHODS));
        assertFalse(response.getHeaders().containsKey(ALLOW_HEADERS));
    }
}
