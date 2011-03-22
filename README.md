MIKEPB JAX-RS Scalate Provider
------------------------------

This package provides a JAX-RS provider and a Servlet filter to allow
[Scalate views](http://scalate.fusesource.org/documentation/user-guide.html#Views)
to be used to render POJO models as HTML pages.

Install
=======

### Build from source

The source for the JAX-RS Scalate provider can be found on
[GitHub](https://github.com/mikepb/jaxrs-scalate). You can get a copy of the
source by cloning the Git repository:

    git clone git://github.com/mikepb/jaxrs-scalate.git

The package is built using Maven:

    mvn package


Usage
=====

### Configure Servlet filter

The `ScalateFilter` Servlet filter intercepts HTTP requests and captures the
Servlet context, request, and response for use by the `ScalateProvider` JAX-RS
provider. As such, `ScalateFilter` must be configured to run before the JAX-RS
filter, if any.

    <web-app>

      ...

      <filter>
        <filter-name>jaxrsScalateFilter</filter-name>
        <filter-class>com.mikepb.jaxrs.scalate.ScalateFilter</filter-class>
      </filter>

      <filter>
        <filter-name>Resteasy</filter-name>
        <filter-class>org.jboss.resteasy.plugins.server.servlet.FilterDispatcher</filter-class>
        <init-param>
          <param-name>javax.ws.rs.Application</param-name>
          <param-value>com.restfully.shop.services.ShoppingApplication</param-value>
        </init-param>
      </filter>

      <filter-mapping>
        <filter-name>jaxrsScalateFilter</filter-name>
        <url-pattern>/*</url-pattern>
      </filter-mapping>

      <filter-mapping>
        <filter-name>Resteasy</filter-name>
        <url-pattern>/*</url-pattern>
      </filter-mapping>

      ...

    </web-app>

Implementation-specific instructions are available for:

- [RESTEasy](http://docs.jboss.org/resteasy/docs/2.0.0.GA/userguide/html/Installation_Configuration.html)


### Configure JAX-RS `Application`

The `ScalateProvider` JAX-RS provider is configured via the `Application`
interface:

    import _root_.scala.collection.JavaConverters._
    import _root_.com.mikepb.jaxrs.scalate.ScalateProvider

    class Application extends javax.ws.rs.core.Application {

      override def getSingletons = Set(
        new ScalateProvider(useCache = true)
      ).asJava.asInstanceOf[java.util.Set[AnyRef]]

      override def getClasses = Set(
        classOf[LandingPageResource]
      ).asJava.asInstanceOf[java.util.Set[java.lang.Class[_]]]

    }

More information is available from the
[JAX-RS JavaDocs](http://jsr311.java.net/nonav/releases/1.1/javax/ws/rs/core/Application.html).

Implementation-specific instructions are available for:

- [RESTEasy](http://docs.jboss.org/resteasy/docs/2.0.0.GA/userguide/html/Installation_Configuration.html#javax.ws.rs.core.Application)


### Create a resource

The JAX-RS API revolves around resources. `LandingPageResource.scala` is an
example resource that displays a message when loading the root URL `/`:

    import javax.ws.rs._

    @Path("/")
    @Produces(Array("text/html"))
    class LandingPageResource {

      @GET
      def index = Message("Hello World!")

    }

    case class Message(message: String)

The Scalate view `Message.index.jade` for `Message`:

    p= message


> Note: only the `index` view name is currently supported.

See the Scalate
[documentation](http://scalate.fusesource.org/documentation/user-guide.html#Views)
for more information about how views work.
