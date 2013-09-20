package com.palominolabs.jersey.cors

import com.google.common.collect.Maps
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.Response
import com.sun.jersey.api.container.filter.LoggingFilter
import com.sun.jersey.api.core.DefaultResourceConfig
import com.sun.jersey.api.core.PackagesResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.spi.container.servlet.ServletContainer
import java.util.logging.LogManager
import javax.ws.rs.GET
import javax.ws.rs.OPTIONS
import javax.ws.rs.Path
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.bridge.SLF4JBridgeHandler

import static com.palominolabs.jersey.cors.CorsHeaders.ALLOW_CREDENTIALS
import static com.palominolabs.jersey.cors.CorsHeaders.ALLOW_HEADERS
import static com.palominolabs.jersey.cors.CorsHeaders.ALLOW_METHODS
import static com.palominolabs.jersey.cors.CorsHeaders.ALLOW_ORIGIN
import static com.palominolabs.jersey.cors.CorsHeaders.EXPOSE_HEADERS
import static com.palominolabs.jersey.cors.CorsHeaders.MAX_AGE
import static com.palominolabs.jersey.cors.Ternary.FALSE
import static com.palominolabs.jersey.cors.Ternary.TRUE

class CorsResourceFilterFactoryTest {
  Server server

  AsyncHttpClient http = new AsyncHttpClient();

  @BeforeClass
  static void setUpLogging() {
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()
  }

  @Before
  public void setUp() {
    server = new Server(8080)

    ServletHolder servletHolder = new ServletHolder(new ServletContainer())
    servletHolder.initParameters.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
        CorsResourceFilterFactory.canonicalName)
    servletHolder.initParameters.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, LoggingFilter.canonicalName)
    servletHolder.initParameters.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, LoggingFilter.canonicalName)
    servletHolder.initParameters.put(PackagesResourceConfig.PROPERTY_PACKAGES, getClass().package.name)
    ServletContextHandler handler = new ServletContextHandler()
    handler.addServlet(servletHolder, '/*')

    server.setHandler(handler)

    server.start()
  }

  @After
  public void tearDown() {
    server.stop()
  }

  @Test
  public void testStandardDefaults() {
    CorsResourceFilterFactory factory = new CorsResourceFilterFactory(new DefaultResourceConfig())

    assert '*' == factory.defAllowOrigin
    assert '' == factory.defExposeHeaders
    assert 24 * 3600 == factory.defMaxAge
    assert FALSE == factory.defAllowCredentials
    assert 'GET' == factory.defAllowMethods
    assert '' == factory.defAllowHeaders
  }

  @Test
  public void testSetDefaultsFromPropertiesWithExactTypes() {
    Map<String, Object> props = Maps.newHashMap()

    props.put(CorsConfig.ALLOW_ORIGIN, 'http://foo.com')
    props.put(CorsConfig.EXPOSE_HEADERS, 'x-foo')
    props.put(CorsConfig.MAX_AGE, 12345)
    props.put(CorsConfig.ALLOW_CREDENTIALS, true)
    props.put(CorsConfig.ALLOW_METHODS, 'POST')
    props.put(CorsConfig.ALLOW_HEADERS, 'x-bar')

    assertOverriddenDefaults(props)
  }

  @Test
  public void testSetDefaultsFromPropertiesWithStrings() {
    Map<String, Object> props = Maps.newHashMap()

    props.put(CorsConfig.ALLOW_ORIGIN, 'http://foo.com')
    props.put(CorsConfig.EXPOSE_HEADERS, 'x-foo')
    props.put(CorsConfig.MAX_AGE, '12345')
    props.put(CorsConfig.ALLOW_CREDENTIALS, 'true')
    props.put(CorsConfig.ALLOW_METHODS, 'POST')
    props.put(CorsConfig.ALLOW_HEADERS, 'x-bar')

    assertOverriddenDefaults(props)
  }

  @Test
  public void testGetUnAnnotated() {
    Response r = http.prepareGet('http://localhost:8080/unAnnotated').execute().get()

    assert 200 == r.statusCode
    assert !r.headers.containsKey(ALLOW_ORIGIN)
    assert !r.headers.containsKey(EXPOSE_HEADERS)
    assert !r.headers.containsKey(MAX_AGE)
    assert !r.headers.containsKey(ALLOW_CREDENTIALS)
    assert !r.headers.containsKey(ALLOW_METHODS)
    assert !r.headers.containsKey(ALLOW_HEADERS)
  }


  @Test
  public void testOptionsUnAnnotated() {
    Response r = http.prepareOptions('http://localhost:8080/unAnnotated').execute().get()

    assert 200 == r.statusCode
    assert !r.headers.containsKey(ALLOW_ORIGIN)
    assert !r.headers.containsKey(EXPOSE_HEADERS)
    assert !r.headers.containsKey(MAX_AGE)
    assert !r.headers.containsKey(ALLOW_CREDENTIALS)
    assert !r.headers.containsKey(ALLOW_METHODS)
    assert !r.headers.containsKey(ALLOW_HEADERS)
  }

  @Test
  public void testGetAnnotatedNoOverrides() {
    Response r = http.prepareGet('http://localhost:8080/annotatedNoOverrides').execute().get()

    assert 200 == r.statusCode
    assert ['*'] == r.headers.get(ALLOW_ORIGIN)
    assert !r.headers.containsKey(ALLOW_CREDENTIALS)
    assert !r.headers.containsKey(EXPOSE_HEADERS)
    assert !r.headers.containsKey(MAX_AGE)
    assert !r.headers.containsKey(ALLOW_METHODS)
    assert !r.headers.containsKey(ALLOW_HEADERS)
  }

  @Test
  public void testOptionsAnnotatedNoOverrides() {
    Response r = http.prepareOptions('http://localhost:8080/annotatedNoOverrides').execute().get()

    assert 200 == r.statusCode

    assert ['86400'] == r.headers.get(MAX_AGE)
    assert ['GET'] == r.headers.get(ALLOW_METHODS)
    assert !r.headers.containsKey(ALLOW_CREDENTIALS)
    assert !r.headers.containsKey(ALLOW_ORIGIN)
    assert !r.headers.containsKey(EXPOSE_HEADERS)
    assert !r.headers.containsKey(ALLOW_HEADERS)
  }

  private static void assertOverriddenDefaults(HashMap<String, Object> props) {
    DefaultResourceConfig config = new DefaultResourceConfig()
    config.setPropertiesAndFeatures(props)

    CorsResourceFilterFactory factory = new CorsResourceFilterFactory(config)

    assert 'http://foo.com' == factory.defAllowOrigin
    assert 'x-foo' == factory.defExposeHeaders
    assert 12345 == factory.defMaxAge
    assert TRUE == factory.defAllowCredentials
    assert 'POST' == factory.defAllowMethods
    assert 'x-bar' == factory.defAllowHeaders
  }

  @Path("unAnnotated")
  static class UnAnnotatedResource {
    @GET
    String get() {
      return 'x'
    }
  }

  @Path("annotatedNoOverrides")
  static class AnnotatedNoOverridesResource {
    @GET
    @Cors
    String get() {
      return 'x'
    }

    @OPTIONS
    @CorsPreflight
    String options() {
      return 'foo'
    }
  }
}
