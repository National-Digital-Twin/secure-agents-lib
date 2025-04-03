# JAX-RS Base Server
The JAX-RS Base Server module provides a bunch of utility code for quickly building new JAX-RS servers.  It provides a
lot of building blocks around the following:

- [Error Handling](#error-handling) and Reporting via [RFC 7807 Problem][1] Responses
- [JWT Authentication](#jwt-authentication)
- [Cross-Origin Resource Sharing (CORS)](#other-utilities)
- [Server configuration initialization](#serverconfiginit)
- [Application stubs](#creating-an-application), [servers](#serverbuilder) and [entrypoints](#creating-an-entrypoint)

## Creating an application
To create a new JAX-RS server add a [Dependency](#dependency) as detailed later and then create a new class extending
`AbstractApplication`.  This class should override `getClasses()`, ensuring that it calls the super-class method and
appends any application specific classes to it e.g.

```java
@ApplicationPath("/")
public class SearchApplication extends AbstractApplication {
    @Override
    public Set<Class<?>> getClasses() {
        // Get the super-class set of classes
        // i.e. all the common functionality the base application class provides
        Set<Class<?>> classes = super.getClasses();

        // Add any application specific functionality
        // Parameter Converters
        classes.add(SearchParamConvertersProvider.class);
        // Resources i.e. actual API paths
        classes.add(DocumentsResource.class);
        return classes;
    }

    @Override
    protected Class<? extends AbstractHealthResource> getHealthResourceClass() {
        // Health Resource, or null if you prefer not to derive from our AbstractHealthResource
        return HealthResource.class;
    }

    @Override
    protected boolean isAuthEnabled() {
      return true;
    }
}
```

If you are implementing a [`/healthz`](#healthz-endpoint) following the libraries provided pattern you can return that
class in your overridden `getHealthResourceClass()` method.  If you prefer to not provide such an endpoint, or implement
it by other means then you can return `null` here.

As seen in the above example another key consideration is whether you want authentication enabled which you can do by
overriding the `isAuthEnabled()` method from the base class.  You may wish to calculate this value based on
environment/system property variables if you want authentication to be configurable as on/off in your application.

## Creating an entrypoint
To create a runnable entrypoint for our application we can extend `AbstractAppEntrypoint` to construct our `Server`
instance and implement a `main()` method on it e.g.

```java
public class SearchApiEntrypoint extends AbstractAppEntrypoint {

    public static void main(String[] args) {
        SearchApiEntrypoint entrypoint = new SearchApiEntrypoint();
        // true causes the JVM to start the server and block until the process is interrupted
        entrypoint.run(true);
    }

    @Override
    protected ServerBuilder buildServer() {
        // In this method we build our server definition
        return ServerBuilder.create()
                .localhost()
                .port(8181)
                .application(SearchApplication.class)
                .withAutoConfigInitialisation()
                .displayName("Search REST API");
    }
}
```

Here we see the [`ServerBuilder`](#serverbuilder) API used to build the definition of our actual server.

## `ServerBuilder`
The `ServerBuilder` API is used to build a definition for a JAX-RS server you want to run. When you call `build()` on it
you get a `Server` instance that can be used to start/stop the server as desired.  At a bare minimum, you **MUST**
configure the Application class, Display name and Port number for the server e.g.

```java
Server server 
  = ServerBuilder.create()
        .application(YourApplication.class)
        .displayName("Example Application")
        .port(10001)
        .build();
```

Would create a server that runs `YourApplication` on `localhost:10001` with a display name of `Example Application`, the
display name is primarily used for logging though also necessary for the application context used to deploy the JAX-RS
application to the server.

By default, your application will be hosted as the root context of the server, if you want to host it under a different
context then you can do so via the `contextPath()` method.  For example `.contextPath("/api")` would host your
application under `/api` on the server.  Note that for inspection purposes the `getBaseUri()` method on the built
`Server` instance will give you the Base URI against which you can formulate your requests including the context path
(if any), this is particularly useful for unit and integration testing.

The hostname for the server is configured via the `hostname("some-host")` or `localhost()` methods, `some-host` can be a
hostname or an IP address.  So you could use `.hostname("0.0.0.0")` to bind the server to all network interfaces on your
host.

The `withAutoConfigInitialisation()` method registers our
[`ServiceLoadedServletContextInitialiser`](../../jaxrs-base-server/src/main/java/uk/gov/dbt/ndtp/secure/agent/server/jaxrs/init/ServiceLoadedServletContextInitialiser.java)
context listener, this automatically detects, loads and runs initialisers that conform to our
[`ServiceConfigInit`](#serverconfiginit) interface.  This module already registers listeners for [JWT
Authentication](#jwt-authentication) and [User Attribute ABAC](#user-attribute-abac) automatically.  Alternatively if
you have existing listeners that do not conform to our interface, but implement the base `ServletContextListener`
interface then you can use these as well via `withListener(YourListener.class)`.

The `withVersionInfo()` method ensures that a libraries [version
information](../observability/index.md#detecting-version-information) is exposed via the applications `/version-info`
endpoint.  If this is not called then only libraries who are creating Open Telemetry meters via
[`IANodeMetrics.getMeter()`](../observability/index.md#obtaining-an-open-telemetry-meter) will be included, and only
if they have done so at the point where a user retrieves `/version-info`.  Calling this method explicitly on your 
`ServerBuilder` ensures that the libraries of relevance to your server application report their version information.

## `Server`
Once you have built a `Server` instance via [`ServerBuilder`](#serverbuilder) you can then start and stop the server via
its `start()` and `stop()` methods, generally you should only start and stop a server once.  If you want to terminate a
server immediately you can do so via the `shutdownNow()` method.

A `Server` is an `AutoCloseable` so for unit or integration tests you can use it with a `try-with-resources` block e.g.

```java
try (Server server = ServerBuilder.create()
                                  .port(1234)
                                  .applicationClass(MyApp.class)
                                  .displayName("Test")
                                  .build()) {
    server.start();

    // Test some server behaviour...

    server.shutdownNow();
}
```

Then if anything goes wrong while the test is running you are guaranteed to clean it up.

## `ServerConfigInit`
The
[`ServerConfigInit`](../../jaxrs-base-server/src/main/java/uk/gov/dbt/ndtp/secure/agent/server/jaxrs/init/ServerConfigInit.java)
interface is a small extension to the `ServletContextListener` interface that adds a name and priority to the interface.
The intent of this is to allow listeners to be automatically discovered (via `ServiceLoader`) and applied in a
well-defined order without having to manually define the listeners.  By using a `ServiceLoader` driven mechanism we need
only define a single explicit listener and provided suitable `META-INF/services` files are present on the Classpath all
our listeners will be discovered and loaded.

When you use a [`ServerBuilder`](#serverbuilder) you can ensure this listener is registered via the
`withAutoConfigInitialisation()` method.

The `getName()` value is used for logging purposes so should be something meaningful to a developer or system
administrator reviewing the logs.

The `priority()` value of these instances allows the initialisation order to be automatically controlled.  A higher
value is considered higher priority and will be initialised first.  When the server is stopped the initialisers are
destroyed in reverse priority order i.e. lowest priority will be destroyed first.

## JWT Authentication
The base server module includes a servlet context listener that configures JWT Authentication using the support from our
[jwt-servlet-auth][2] module.  This listener looks for an environment variable named `JWKS_URL` and uses its value to
configure a suitable [`JwtVerifier`][3] for your application.  This will also configure an authentication engine that
sends challenge responses using RFC 7807 Problem Responses for alignment with the [error handling](#error-handling)
provided by this module.

The `JWKS_URL` environment variable is used to configure the location of the public keys at server startup.  This may
either be a remote URL or a local file, for a local file the URL must be of the form `file:///path/to/jwks.json`.
Alternatively it may be one of several special values:

- `disabled` - Disables authentication entirely.  This just means that no verifier will be registered, if your
  application does not also disable authentication in its application class definition the relevant request filter will
  still be installed and result in requests being rejected as unauthenticated.
- `aws:<region>` - Enables AWS authentication where `<region>` is the AWS region in which the server is being deployed.
  This will configure authentication to use the custom AWS headers and resolve public keys from AWS ELB.

Since `0.16.0` the following additional environment variables may be used for further configuration:

| Environment Variable | Example Value | Notes |
|----------------------|---------------|-------|
| `JWT_HEADERS_NAMES`  | `Authorization,X-Custom` | Specifies a comma separated list of HTTP Header names that will be searched to find a suitable JWT |
| `JWT_HEADERS_PREFIXES` | `Bearer,` | Specifies a comma separated list of prefixes that are present in the HTTP Headers and will be stripped as part of extracting the token |
| `JWT_USERNAME_CLAIMS` | `email,username` | Specifies a comma separated list of claim names that will be used in extracting the username of the authenticated user from the verified JWT |

In the example values shown in this table we allow the token to be presented in either the `Authorization` or the
`X-Custom` header.  For the `Authorization` header it is expected to have a prefix of `Bearer` present, e.g. `Bearer
<jwt>`, while the `X-Custom` header is expected to have no prefix and just contain the token, e.g. `<jwt>`.

Assuming that we successfully verify a JWT then we will search first the `email`, and then the `username` claim to find
the username for the user.  Note that regardless of the claims configured here if none are present the authentication
library falls back to using the JWT standard `sub` (subject) claim to detect a user identity.

## User Attribute ABAC
The User Attributes Service is part of Integration Architecture Node (IA Node)  and provides the ability to look-up the attributes for an
authenticated user in order to then enforce Attribute Based Access Control (ABAC) upon a users requests.  [IA Node
Access](https://github.com/National-Digital-Twin/ianode-access) is the default implementation of this service for IA Node.

To use a User Attributes service you must set the `USER_ATTRIBUTES_URL` environment variable to the URL of the
attributes service.  For example if you had deployed Access entirely locally it would be
`https://localhost:8091/users/lookup/{user}`.  Note that your URL must include `{user}` as the placeholder where the
username should be substituted in order to derive a URL that looks up the users attributes.

Alternatively it may be the special value `disabled` which disables user attributes lookup, internally this configures
the user attributes store to be an empty store so all users have no attributes.

**NB:** `disabled` **MUST** only be used for local development and testing.

## Error Handling
The module provides a number of `ExceptionMapper` instances that are automatically registered when you derive from
`AbstractApplication` per [Creating an Application](#creating-an-application).  These handle the following errors,
turning them into [RFC 7807 Problem][1] responses:

- Constraint Violations i.e. when a request parameter does not pass Bean validation rules declared on its method
  parameters
- Method Not Allowed i.e. when a user attempts to use an HTTP Verb on a request path that does not accept that verb.
  For example if they attempted a `DELETE` on a request that only permits `GET`.
- Not Found i.e. 404 errors when a user provides a bad URI, or the URI does not identify a resource that exists.
- Parameter Conversion Exceptions i.e. when a user provides a value to a request parameter that cannot be converted into
  the relevant type.

Additionally, it also installs a generic fallback mapper that will turn any other errors into problem responses. This
ensures consistent error handling behaviour of our application servers.

Problem responses are based upon [RFC 7807][1] and by default are a JSON structure and will be serialized as such if an
endpoint method that produces one as a response declares `application/json` or `application/problem+json` in its
`@Produces` annotation.  From 0.25.0 onwards we also support serializing a subset of the problem into `text/plain`
responses.  If an endpoint method does not declare one of the supported media types, or supports other media types, then
your application may need to provide its own additional `MessageBodyWriter<Problem>` for those media types.

## Other Utilities
There are a few other minor utilities provided by this library.

### CORS
When [building your server](#serverbuilder) you can use the `withCors()` method to provide/manipulate a
`CorsConfigurationBuilder` object that allows you to set up the necessary CORS configuration for your application.  By
default, CORS is enabled for all servers unless `withoutCors()` is explicitly called on the `ServerBuilder`.

The default CORS configuration allows all standard HTTP methods, the `Accept`, `Authorization`, `Content-Disposition`,
`Content-Type` and `Request-ID` headers to be included in pre-flight requests and the `Request-ID` header to be obtained
from pre-flight responses.

**NB:** Remember that a number of HTTP Headers are implicitly permitted by the [CORS Request safe-list][4] and the [CORS
Response safe-list][5].

If your application requires custom CORS settings then use the `withCors()` method to configure CORS as desired e.g.

```java
Server server
  = ServerBuilder.create()
                 .localhost()
                 .port(12345)
                 .application(ExampleApplication.class)
                 .withAutoConfigInitialisation()
                 .withCors(c -> c.addAllowedHeaders("X-Custom")
                                 .addExposedHeaders("X-Custom")
                                 .addAllowedOrigins("https://your-domain.com")
                                 .preflightMaxAge(30))
                 .displayName("Custom CORS API")
```

### Request IDs
The `RequestIdFilter` is automatically registered when you derive from `AbstractApplication` per [Creating an
Application](#creating-an-application). It adds a unique `Request-ID` header containing a UUID to each incoming request
and copies this header to the outgoing response as well.  The Request ID is also copied into the logging `MDC` allowing
logging configuration to include it as part of the log format, this then allows the logs to be correlated with
individual requests. This is particularly useful when many users are making concurrent requests to the server and the
logging for those requests may be arbitrarily interleaved.

#### Client Provided Request IDs
If a client provides a `Request-ID` header in their original request then this is used as the prefix for creating a
unique Request ID by appending an incrementing number (seeded from the server startup time) to the client provided ID.
Thus resulting in Request IDs like `test/1677839349485`, `test/1677839349486` etc.

This feature is useful if clients need to make multiple requests to perform some logical operation (from the client's
viewpoint) and wants to later correlate all those requests within the logs.  Obviously a client needs to be careful that
their supplied Request ID is sufficiently unique to start with, e.g. by generating its own UUIDs. 

Note that in order to protect the server from overly long Request IDs the server imposes a maximum length of 36
characters on client provided IDs.  This corresponds to the length of a standard UUID encoded as a string e.g.
`d34b6a52-0511-42d8-9211-819bca4db626`.  If the client provided Request ID is longer than this is will be truncated to
36 characters and then the server provided unique suffix appended as shown above.

### Result Paging
The `Paging` static class provides an `applyPaging()` method that can be used to apply limit and offset based paging to
any list of results.

Note that generally it will be better to implement paging directly in your underlying APIs wherever possible, this is
merely a stop-gap measure for the scenario where such capabilities are not supported by an API.

### `/healthz` endpoint
The `AbstractHealthResource` provides a JAX-RS resource class for implementing the `/healthz` health endpoint in your
application.  You need to derive from this class and override the `determineStatus()` method to provide a `HealthStatus`
object describing the health of your server.  

From `0.22.0` onwards you can provide your class for this via overriding the abstract `getHealthResourceClass()` method
in your application class.  Note that if you prefer not to use our `AbstractHealthResource` as a base then you should
return `null` from that method, and just register your own health resource class in your applications
[`getClasses()`](#creating-an-application) method.

### `/version-info` endpoint
The `VersionInfoResource` provides a `/version-info` endpoint to your server that exposes version information.  You can
ensure specific libraries information is exposed there by using the `withVersionInfo("library-name")` method on the
[`ServerBuilder`](#serverbuilder).  See the [Detecting Version
Information](../observability/index.md#detecting-version-information) documentation for details on how the version
information is detected.

# Dependency
This API is provided by the `jaxrs-base-server` module which can be depended on from Maven like so:

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>jaxrs-base-server</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

# Testing
The `tests` classifier of this module includes some useful mock servers that can be used in setting up testing of
servers built with this framework.

## Mock Key Server
For testing that services properly enable authentication and can verify user identity there is a `MockKeyServer` that
can be instantiated and started like so:

```java
MockKeyServer keyServer = new MockKeyServer(12345);
keyServer.start();

// Pick one of the available Key IDs
String keyId = keyServer.getKeyIdsAsList().get(0);

// Create a JWT signed with a key provided by the key server
String jwt = Jwts.builder()
                 .header()
                 .keyId(keyId)
                 .and()
                 .subject("user@example.org")
                 .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                 .signWith(keyServer.getPrivateKey(keyId))
                 .compact();

// Create a verifier that can verify keys from the key server
JwtVerifier verifier 
  = new SignedJwtVerifier(new CachedJwksKeyLocator(URI.create(keyServer.getJwksUrl()), 
                                                   Duration.ofMinutes(15)));

// Verify the signed JWT
Jws<Claims> jws = verifier.verify(jwt);
```

It provides a `getJwksUrl()` method that provides a JWKS URL that can be used to configure the JWT Authentication
Filter, see [JWT Authentication](#jwt-authentication) for configuration details.

If you want to mock an AWS deployment you can call `registerAsAwsRegion("test")` and then configure the filter with the
`test` region to have it resolve keys AWS style from the key server.  You should remember to call
`AwsElbKeyUrlRegistry.reset()` after your tests to remove the custom configuration.

Finally you should always call `stop()` in your test class teardown to stop the mock key server.

## Mock Attributes Server
There is also a `MockAttributesServer` provided that allows mocking out the user attribute service, this allows for
testing services that need to retrieve user attributes and make authorization decisions based upon those.

```java
// Create an attributes store with the attributes you want to serve for your test(s)
AttributeValueSet attributes 
  = AttributeValueSet.of(
    List.of(
      AttributeValue.of("name", ValueTerm.value("Thomas T. Test")),
      AttributeValue.of("admin", ValueTerm.value(true))));
AttributesStoreLocal local = new AttributesStoreLocal();
local.put("test", expected);

// Create and start the mock server with that store
MockAttributesServer attributesServer = new MockAttributesServer(local);
attributesServer.start();

// Can then create a remote attributes store backed by it
AttributesStore remote 
  = new AttributesStoreRemote(attributesServer.getUserLookupUrl(), 
                              attributesServer.getHierarchyLookupUrl());

// And lookup user attributes
AttributeValueSet userAttributes = remote.attributes("test");

// Stop the server
attributesServer.shutdown();
```

The server provides a `getUserLookupUrl()` and a `getHierarchyLookupUrl()` method that can be used to obtain the URLs to
pass into a service to configure it to use the mock server, see [User Attributes](#user-attribute-abac) for
configuration details.

[1]: https://datatracker.ietf.org/doc/html/rfc7807
[2]: https://github.com/National-Digital-Twin/jwt-servlet-auth
[3]: https://github.com/National-Digital-Twin/jwt-servlet-auth#verifiers
[4]: https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_request_header
[5]: https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_response_header
