This library makes it easy to add basic CORS support to your [Jersey 1](https://jersey.java.net/) app. To learn more about CORS, see documentation from [MDN](https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS) and [W3](http://www.w3.org/TR/cors/).

# Usage
To get started, add a dependency to your Gradle build file:

    compile com.palominolabs.jersey:jersey-cors-filter:VERSION

where `VERSION` is the latest released version.  If you're using Maven, know that your life could be greatly improved by switching to Gradle and use this dependency block:

    <dependency>
        <groupId>com.palominolabs.jersey</groupId>
        <artifactId>jersey-cors-filter</artifactId>
        <version>VERSION</version>
    </dependency>


## Registering the filter with Jersey
This library includes a `ResourceFilterFactory` implementation: [`CorsResourceFilterFactory`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsResourceFilterFactory.java). You need to inform Jersey that you want to use this as a filter. Typically you would do this by setting the Jersey init param `com.sun.jersey.spi.container.ResourceFilters` (which, if using the servlet/Jersey integration, can be done by setting servlet init params); the param name is also available more conveniently in code as `ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES`.

### With embedded Jetty's ServletContainer
This is how to do it when using the jersey-servlet `ServletContainer` servlet with embedded Jetty:

    ServletHolder servletHolder = new ServletHolder(new ServletContainer());
    servletHolder.initParameters.put(
            ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            CorsResourceFilterFactory.class.getCanonicalName()
    );

### With Guice
If your app uses [Guice](http://code.google.com/p/google-guice/), you need to add the CorsResourceFilterFactory to the properties used for instantiating the GuiceContainer.  In your ServletModule.configureServlets():

    bind(GuiceContainer.class);
    HashMap<String, String> guiceContainerProps = Maps.newHashMap();
    guiceContainerProps.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES,
            CorsResourceFilterFactory.class.getCanonicalName());

    serve("/*").with(GuiceContainer.class, guiceContainerProps);

## Configuring CORS headers
Once the filter is registered, you can annotate your resource methods (`@GET`, `@POST`, etc.) with [`@Cors`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/Cors.java) to send basic resource response headers and your `@OPTIONS` methods with [`@CorsPreflight`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsPreflight.java) to send preflight request response headers.

    @Path("foo")
    class FooResource {
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

You can also apply `@Cors` and `@CorsPreflight` to resource classes, which will act as if you applied `@Cors` to all non-`@OPTIONS` methods and `@CorsPreflight` to all `@OPTIONS` methods, respectively.

    // Equivalent to above resource class
    @Path("foo")
    @Cors
    @CorsPreflight
    class FooResource {
        @GET
        String get() {
          return 'x'
        }

        @OPTIONS
        String options() {
          return 'foo'
        }
    }

## Custom CORS headers
Out of the box, the filter will send `Access-Control-Allow-Origin: *` for methods annotated `@Cors`, while methods annotated `@CorsPreflight` will send  `Access-Control-Allow-Methods: GET` and `Access-Control-Max-Age: 86400`. This is plenty for most purposes, allowing `GET` requests from anywhere and instructing the user agent (i.e. the browser) cache the results of the CORS for 24 hours.

If you need to change those defaults, or specify other headers like `Access-Control-Allow-Headers`, [`CorsConfig`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsConfig.java) defines various param names to set. These are loaded by the filter out of the standard Jersey property mechanism. If you're using Jersey via the Servlet API, setting servlet init params should do the trick.

    Map<String, Object> props = Maps.newHashMap()
    props.put(CorsConfig.ALLOW_ORIGIN, 'http://foo.com')
    props.put(CorsConfig.EXPOSE_HEADERS, 'x-foo')
    props.put(CorsConfig.MAX_AGE, 12345)
    props.put(CorsConfig.ALLOW_CREDENTIALS, true)
    props.put(CorsConfig.ALLOW_METHODS, 'POST')
    props.put(CorsConfig.ALLOW_HEADERS, 'x-bar')

    DefaultResourceConfig config = new DefaultResourceConfig()
    config.setPropertiesAndFeatures(props)

    CorsResourceFilterFactory factory = new CorsResourceFilterFactory(config)

    // Register your FilterFactory with the ServletHolder as above

## Overriding with annotations
If you need to override any of these settings for a method or class, you can do so via the optional values on `@Cors` and `@CorsPreflight`, as in `@Cors(exposeHeaders = "X-FooBar")`. Values specified on method annotations take precedence over class annotations.


    @Path("foo")
    @Cors(
        allowCredentials = FALSE,
        allowOrigin = 'http://asdfasdf.com',
        exposeHeaders = 'x-asdfasdf'
    )
    @CorsPreflight(
        allowCredentials = FALSE,
        allowHeaders = 'x-asdfasdf',
        allowMethods = 'DELETE',
        maxAge = 54321,
        allowOrigin = 'http://foo.com'
    )
    static class FooResource {
        @GET
        @Cors(
            allowCredentials = TRUE,
            allowOrigin = 'http://foo.com',
            exposeHeaders = 'x-foo'
        )
        String get() {
            return 'x'
        }

        @POST
        @Cors(
            allowCredentials = TRUE,
            allowOrigin = 'http://foo.com',
            exposeHeaders = 'x-foo'
        )
        String post() {
            return 'y'
        }

        @OPTIONS
        @CorsPreflight(
            allowCredentials = TRUE,
            allowHeaders = 'x-foo',
            allowMethods = 'GET,POST',
            maxAge = 12345
        )
        String options() {
            return 'foo'
        }
    }

## More
See the [CorsResourceFilterFactory test](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/test/groovy/com/palominolabs/jersey/cors/CorsResourceFilterFactoryTest.groovy) for complete examples of how the jersey-cors-filter can be used.
