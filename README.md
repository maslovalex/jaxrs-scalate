MIKEPB JAX-RS Scalate Provider
------------------------------

This package provides a JAX-RS provider to allow
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

The Scalate `TemplateEngineFilter` Servlet filter initializes the template
engine and can be configured anywhere in the filter chain. A subclass of the
filter can also be used, so long as the `TemplateEngineFilter.doFilter` method
is invoked properly.

    <web-app>

      ...

      <filter>
        <filter-name>Resteasy</filter-name>
        <filter-class>org.jboss.resteasy.plugins.server.servlet.FilterDispatcher</filter-class>
        <init-param>
          <param-name>javax.ws.rs.Application</param-name>
          <param-value>MyApplication</param-value>
        </init-param>
      </filter>

      <filter>
        <filter-name>TemplateEngineFilter</filter-name>
        <filter-class>org.fusesource.scalate.servlet.TemplateEngineFilter</filter-class>
      </filter>

      <filter-mapping>
        <filter-name>Resteasy</filter-name>
        <url-pattern>/*</url-pattern>
      </filter-mapping>

      <filter-mapping>
        <filter-name>TemplateEngineFilter</filter-name>
        <url-pattern>/*</url-pattern>
      </filter-mapping>

      ...

    </web-app>

Implementation-specific instructions are available for:

- [RESTEasy](http://docs.jboss.org/resteasy/docs/2.0.0.GA/userguide/html/Installation_Configuration.html)


### Configure JAX-RS

The `ScalateProvider` JAX-RS provider is configured via the `Application`
interface:

    import _root_.com.mikepb.jaxrs.scalate.JaxrsScalateProvider

    class MyApplication extends Application with JaxrsScalateApplication {
      override def getClasses: java.util.Set[Class[_]] = {
        classes.add(classOf[LandingPageResource])
        classes.addAll(super.getClasses)
        classes
      }
    }

More information is available from the
[JAX-RS JavaDocs](http://jsr311.java.net/nonav/releases/1.1/javax/ws/rs/core/Application.html).

Implementation-specific instructions are available for:

- [RESTEasy](http://docs.jboss.org/resteasy/docs/2.0.0.GA/userguide/html/Installation_Configuration.html#javax.ws.rs.core.Application)


### Create a resource

To render POJO objects using Scalate views, use the JAX-RS `@Produces`
annotation to tell the JAX-RS implementation that you want the returned POJO
should be rendered using the Scalate provider.

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

To use a view other than "index", use the `@ViewName` annotation to provide
the view name:

    import javax.ws.rs._
    import com.mikepb.jaxrs.scalate.ViewName

    @Path("/")
    @Produces(Array("text/html"))
    @ViewName("custom")
    class LandingPageResource {

      @GET
      def index = this

    }

The Scalate view `LandingPageResource.custom.jade` for `LandingPageResource`:

    :markdown
      This is a custom view!

See the Scalate
[documentation](http://scalate.fusesource.org/documentation/user-guide.html#Views)
for more information about how views work.
