/*
 * Copyright 2011 Michael Phan-Ba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mikepb.jaxrs.scalate

import _root_.scala.collection.JavaConversions._
import _root_.scala.collection.mutable.Map
import _root_.java.io._
import _root_.java.lang.Class
import _root_.java.lang.annotation.Annotation
import _root_.java.lang.reflect.Type
import _root_.java.util.LinkedHashMap
import _root_.javax.servlet._
import _root_.javax.servlet.http._
import _root_.javax.ws.rs._
import _root_.javax.ws.rs.core._
import _root_.javax.ws.rs.ext._
import _root_.org.fusesource.scalate._
import _root_.org.fusesource.scalate.servlet._

/**
 * Scalate JAX-RS provider.
 *
 * To use this provider, register it using [[javax.ws.rs.core.Application]]
 * and configure your Servlet container with the
 * [[org.fusesource.scalate.servlet.TemplateEngineFilter]] Servlet filter.
 * The Scalate servlet filter initializes the template engine.
 *
 * See your JAX-RS container documentation and the
 * [[http://scalate.fusesource.org/documentation/user-guide.html#using_scalate_as_servlet_filter_in_your_web_application Scalate Servlet feature]]
 * for more information.
 *
 * {{{
 * class GCApplication extends javax.ws.rs.core.Application {
 *
 *   override def getSingletons = Set(
 *     new ScalateProvider(useCache = true)
 *   ).asJava.asInstanceOf[java.util.Set[AnyRef]]
 *
 *   override def getClasses = Set(
 *     classOf[LandingPageResource]
 *   ).asJava.asInstanceOf[java.util.Set[java.lang.Class[_]]]
 *
 * }
 * }}}
 *
 * Template look-ups are not cached by default. To enable caching, set the
 * `useCache` constructor parameter as shown above.
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
@Provider
@Produces(Array(MediaType.TEXT_HTML))
class ScalateProvider(useCache: Boolean = false) extends MessageBodyWriter[AnyRef] {

  @Context
  private var request: HttpServletRequest = _

  @Context
  private var response: HttpServletResponse = _

  @Context
  private var servletContext: ServletContext = _

  private lazy val engine = ServletTemplateEngine(servletContext)

  private val templateLookupCache =
    if (useCache)
      new LinkedHashMap[String, Boolean](128, 1.1f, true) {
        override protected def removeEldestEntry(x: java.util.Map.Entry[String, Boolean]) = size > 1024
      }: Map[String, Boolean]
    else null

  /**
   * Determines if the POJO model has a Scalate view template.
   *
   * @param klass the POJO model class
   * @param viewName the Scalate view name
   * @return true if a template is available, false otherwise
   * @since 1.0
   */
  private def templateExists(klass: Class[_], viewName: String): Boolean = {
    val templateName = "/%s.%s.".format(klass.getName.replace(".", "/"), viewName)
    for (base <- Array("/WEB-INF", ""); path <- TemplateEngine.templateTypes.map(templateName + _))
      if (engine.resourceLoader.exists(base + path)) return true
    false
  }

  /**
   * Determines if the POJO model has a Scalate view template, caching the results.
   *
   * @param klass the POJO model class
   * @param viewName the Scalate view name
   * @return true if a template is available, false otherwise
   * @since 1.0
   */
  private def templateExistsFromCache(klass: Class[_], viewName: String) = {
    val cacheKey = klass.getName + "$" + viewName
    templateLookupCache synchronized {
      templateLookupCache.getOrElse(cacheKey, {
        val answer = templateExists(klass, viewName)
        templateLookupCache += ((cacheKey, answer))
        answer
      })
    }
  }

  /**
   * Retrieves the view name from annotations.
   *
   * @param annotations the annotations
   * @return the view name given by @[[ViewName]] or "index"
   * @since 1.0
   */
  private def viewNameFromAnnotations(annotations: Array[Annotation]): String = {
    for (annotation <- annotations) {
      annotation match {
        case vn: ViewName => return vn.value
        case _ =>
      }
    }
    "index"
  }

  /**
   * Determines if the POJO model has a Scalate view template.
   *
   * @since 1.0
   * @see [[javax.ws.rs.ext.MessageBodyWriter.isWriteable]]
   */
  def isWriteable(kind: Class[_], genericType: Type, annotations: Array[Annotation],
                  mediaType: MediaType) = {
    val componentType = kind.getComponentType
    val klass = if (componentType != null) componentType else kind
    val viewName = viewNameFromAnnotations(annotations)
    if (useCache && templateLookupCache != null) templateExistsFromCache(klass, viewName)
    else templateExists(klass, viewName)
  }

  /**
   * Determines the size of the rendered template, currently indeterminate.
   *
   * @since 1.0
   * @see [[javax.ws.rs.ext.MessageBodyWriter.getSize]]
   */
  def getSize(model: AnyRef, kind: Class[_], genericType: Type, annotations: Array[Annotation],
              mediaType: MediaType) = -1L

  /**
   * Renders the POJO model view.
   *
   * @since 1.0
   * @see [[javax.ws.rs.ext.MessageBodyWriter.writeTo]]
   */
  def writeTo(model: AnyRef, kind: Class[_], genericType: Type, annotations: Array[Annotation],
              mediaType: MediaType, httpHeaders: MultivaluedMap[String, AnyRef],
              entityStream: OutputStream) {
    val out = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"))
    val context = new ServletRenderContext(engine, out, request, response, servletContext)
    val viewName = viewNameFromAnnotations(annotations)
    val traversable: Traversable[AnyRef] = model match {
      case model: Array[AnyRef] => model.toList
      case model: Traversable[AnyRef] => model
      case model: java.lang.Iterable[AnyRef] => model
      case _ => null
    }

    engine.layout(new Template {
      def render(context: RenderContext) = {
        if (traversable != null) context.collection(traversable, viewName)
        else context.view(model, viewName)
      }
    }, context)

    out.flush
  }
}
