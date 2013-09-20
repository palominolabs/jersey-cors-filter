This library makes it easy to add basic CORS support to your [Jersey](https://jersey.java.net/) app. It is compatible with Jersey 1. Jersey 2 still lacks some features that [we](http://palominolabs.com) use, but as Jersey 2 catches up we'll add support for that as well.

# Usage
To get started, add a dependency on `com.palominolabs.jersey:jersey-cors-filter:VERSION` where `VERSION` is the latest released version.

## Registering the filter with Jersey
This library includes a `ResourceFilterFactory` implementation: [`CorsResourceFilterFactory`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsResourceFilterFactory.java). You need to inform Jersey of this class. Typically you would do this by setting the Jersey init param `com.sun.jersey.spi.container.ResourceFilters` (which, if using the servlet/Jersey integration, can be done by setting servlet init params); the param name is also available more conveniently in code as `ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES`.

## Configuring CORS headers
Once the filter is registered, you can annotate your resource methods (`@GET`, `@POST`, etc.) with [`@Cors`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/Cors.java) to send basic resource response headers and your `@OPTIONS` methods with [`@CorsPreflight`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsPreflight.java) to send preflight request response headers.

Out of the box, the filter will send `Access-Control-Allow-Origin: *` for methods annotated `@Cors`, while methods annotated `@CorsPreflight` will send  `Access-Control-Allow-Methods: GET` and `Access-Control-Max-Age: 86400`. This is plenty for most people: allow `GET` requests from anywhere and instruct the user agent cache the results for 24 hours.

If you need to change those defaults, or specify other headers like `Access-Control-Allow-Headers`, [`CorsConfig`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsConfig.java) defines various param names to set. These are loaded by the filter out of the standard Jersey property mechanism. If you're using Jersey via the Servlet API, setting servlet init params should do the trick.

If you need to override any of these settings for a method, you can do so via the optional values on `@Cors` and `@CorsPreflight`.