This library makes it easy to add basic CORS support to your [Jersey 1](https://jersey.java.net/) app. To learn more about CORS, see documentation from [MDN](https://developer.mozilla.org/en-US/docs/HTTP/Access_control_CORS) and [W3](http://www.w3.org/TR/cors/).

# Usage
To get started, add a dependency on `com.palominolabs.jersey:jersey-cors-filter:VERSION` where `VERSION` is the latest released version.

## Registering the filter with Jersey
This library includes a `ResourceFilterFactory` implementation: [`CorsResourceFilterFactory`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsResourceFilterFactory.java). You need to inform Jersey that you want to use this as a filter. Typically you would do this by setting the Jersey init param `com.sun.jersey.spi.container.ResourceFilters` (which, if using the servlet/Jersey integration, can be done by setting servlet init params); the param name is also available more conveniently in code as `ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES`.

## Configuring CORS headers
Once the filter is registered, you can annotate your resource methods (`@GET`, `@POST`, etc.) with [`@Cors`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/Cors.java) to send basic resource response headers and your `@OPTIONS` methods with [`@CorsPreflight`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsPreflight.java) to send preflight request response headers. You can also apply `@Cors` and `@CorsPreflight` to resource classes, which will be as if you applied `@Cors` to all non-`@OPTIONS` methods and `@CorsPreflight` to all `@OPTIONS` methods, respectively.

Out of the box, the filter will send `Access-Control-Allow-Origin: *` for methods annotated `@Cors`, while methods annotated `@CorsPreflight` will send  `Access-Control-Allow-Methods: GET` and `Access-Control-Max-Age: 86400`. This is plenty for most people: allow `GET` requests from anywhere and instruct the user agent cache the results for 24 hours.

If you need to change those defaults, or specify other headers like `Access-Control-Allow-Headers`, [`CorsConfig`](https://github.com/palominolabs/jersey-cors-filter/blob/master/src/main/java/com/palominolabs/jersey/cors/CorsConfig.java) defines various param names to set. These are loaded by the filter out of the standard Jersey property mechanism. If you're using Jersey via the Servlet API, setting servlet init params should do the trick.

If you need to override any of these settings for a method, you can do so via the optional values on `@Cors` and `@CorsPreflight`, as in `@Cors(exposeHeaders = "X-FooBar")`. Values specified on method annotations take precedence over class annotations.