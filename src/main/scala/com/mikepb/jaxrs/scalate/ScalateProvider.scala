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
import _root_.java.io.{OutputStream, PrintWriter}
import _root_.java.lang.Class
import _root_.java.lang.annotation.Annotation
import _root_.java.lang.reflect.Type
import _root_.java.util.LinkedHashMap
import _root_.javax.ws.rs._
import _root_.javax.ws.rs.core._
import _root_.javax.ws.rs.ext._
import _root_.org.fusesource.scalate._
import _root_.org.fusesource.scalate.servlet._

/**
 * Scalate JAX-RS provider.
 *
 * To use this provider, register it using [[javax.ws.rs.core.Application]]
 * and configure your Servlet container with the [[ScalateProvider.ScalateFilter]]
 * Servlet filter to run before the JAX-RS provider filter, if any.
 *
 * See your JAX-RS container documentation for more information.
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
class ScalateProvider(val useCache: Boolean = false) extends MessageBodyWriter[AnyRef] {

  import ScalateProvider._

  private val _cache =
    if (useCache)
      new LinkedHashMap[String, Boolean](128, 1.1f, true) {
        override protected def removeEldestEntry(x: java.util.Map.Entry[String, Boolean]) = size > 1024
      }: Map[String, Boolean]
    else null

  /**
   * Determines if the POJO model has a Scalate view template, caching results.
   *
   * @since 1.0
   */
  def hasTemplate(kind: Class[_], viewName: String = "index"): Boolean = {
    if (useCache && _cache != null) {
      val cacheKey = kind.getName + "$" + viewName
      _cache synchronized {
        return _cache.getOrElse(cacheKey, {
          val answer = ScalateProvider.hasTemplate(kind)
          _cache += ((cacheKey, answer))
          answer
        })
      }
    }
    ScalateProvider.hasTemplate(kind)
  }

  /**
   * Determines if the POJO model has a Scalate view template.
   *
   * @since 1.0
   * @see [[javax.ws.rs.ext.MessageBodyWriter.isWriteable]]
   */
  def isWriteable(kind: Class[_], genericType: Type, annotations: Array[Annotation],
                  mediaType: MediaType) = hasTemplate(kind)

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
    render(model, new PrintWriter(entityStream))
  }
}

/**
 * Scalate JAX-RS provider companion object.
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
object ScalateProvider {

  private[scalate] val _engine = new ThreadLocal[TemplateEngine]
  private[scalate] val _context = new ThreadLocal[PrintWriter => ServletRenderContext]

  /**
   * Determines if the POJO model has a Scalate view template.
   *
   * @param klass the POJO model class
   * @param viewName the Scalate view name
   * @return true if a template is available, false otherwise
   */
  def hasTemplate(klass: Class[_], viewName: String = "index"): Boolean = {
    _engine.get match {
      case null =>
        throw new RuntimeException("No template engine found")
      case engine =>
        val templateName = "/%s.%s.".format(klass.getName.replace(".", "/"), viewName)
        for (base <- Array("/WEB-INF", ""); path <- TemplateEngine.templateTypes.map(templateName + _))
          if (engine.resourceLoader.exists(base + path)) return true
        false
    }
  }

  /**
   * Renders the model view using the JAX-RS output.
   *
   * This method works only if the Scalate servlet filter helper has
   * intercepted the HTTP request for the calling thread.
   *
   * @param it the POJO model
   * @param out the JAX-RS [[java.io.PrintWriter]]
   * @since 1.0
   */
  def render(it: AnyRef, out: PrintWriter) {
    _context.get match {
      case null =>
        throw new RuntimeException("No template context found for it: " + it.getClass)
      case getContext =>
        val context = getContext(out)
        context.view(it)
        context.flush
    }
  }
}
