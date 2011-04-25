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

import _root_.org.fusesource.scalate.{AttributeMap, DefaultRenderContext, TemplateEngine}

import _root_.scala.collection.JavaConversions._
import _root_.scala.collection.{Iterator, Set}

import _root_.javax.servlet.ServletContext
import _root_.javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import _root_.javax.ws.rs.core.{UriBuilder, UriInfo}

import _root_.java.io.PrintWriter
import _root_.java.net.URI
import _root_.java.util.Locale

/**
 * JAX-RS render context provides JAX-RS-specific methods for templates.
 *
 * Based on [[org.fusesource.scalate.servlet.ServletRenderContext]].
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
class JaxrsRenderContext(val servletContext: ServletContext,
                         val request: HttpServletRequest,
                         val response: HttpServletResponse,
                         val uriInfo: UriInfo,
                         engine: TemplateEngine, out: PrintWriter)
  extends DefaultRenderContext(request.getRequestURI, engine, out) {

  def this(servletContext: ServletContext, request: HttpServletRequest, response: HttpServletResponse,
           uriInfo: UriInfo, engine: TemplateEngine) {
    this (servletContext, request, response, uriInfo, engine, response.getWriter)
  }

  private val self = this

  viewPrefixes = List("WEB-INF", "")

  /**
   * Request attributes access.
   */
  override val attributes = new AttributeMap[String, Any] {
    def get(key: String): Option[Any] = Option(apply(key))
    def apply(key: String): Any = key match {
      case "context" => self
      case _ => request.getAttribute(key)
    }
    def update(key: String, value: Any) {
      value match {
        case null => request.removeAttribute(key)
        case _ => request.setAttribute(key, value)
      }
    }
    def remove(key: String) = {
      val answer = get(key)
      if (answer.isDefined) request.removeAttribute(key)
      answer
    }
    def keySet: Set[String] = request.getAttributeNames.toSet.asInstanceOf[Set[String]]
  }

  /**
   * Request parameters access (from query string or form).
   */
  val parameters = new Iterable[(String, Array[String])] {
    def apply(name: String): String = get(name).get
    def get(name: String): Option[String] = Option(request.getParameter(name))
    def getValues(name: String): Array[String] = request.getParameterValues(name) match {
      case null => Array.empty
      case value => value
    }
    def keySet: Set[String] = request.getParameterNames.toSet.asInstanceOf[Set[String]]
    def iterator: Iterator[(String, Array[String])] = {
      request.getParameterMap.iterator.asInstanceOf[Iterator[(String, Array[String])]]
    }
  }

  /**
   * Request locale or system default.
   */
  override def locale: Locale = Option(request.getLocale).getOrElse(super.locale)

  /**
   * Request URI.
   */
  override def requestUri: String = request.getRequestURI

  /**
   * Request query string.
   */
  def queryString: String = request.getQueryString

  /**
   * Context path.
   */
  def contextPath: String = request.getContextPath

  /**
   * URI for this context path.
   */
  override def uri(uri: String) = if (uri.startsWith("/")) request.getContextPath + uri else uri

  // ---------------------------------------------------------- JAX-RS URI

  /**
   * Gets a new instance of JAX-RS [[UriBuilder]] for the current URI.
   */
  def uriBuilder: UriBuilder = uriInfo.getBaseUriBuilder

  /**
   * Build a URI for this class.
   * @tparam R the class for which to build a URI
   */
  def uri[R: Manifest]: URI = uriBuilder.path(manifest[R].erasure).build()

  /**
   * Build a URI for this class with options.
   *
   * If a query parameter is supplied as a [[Seq]], multiple values will
   * be set. Otherwise, the value is converted to a string.
   *
   * @tparam R the class for which to build a URI
   * @param method the optional method for which to build a URI
   * @param query the optional URI query params
   * @param fragment the optional URI fragment
   * @param port the optional URI port
   * @param absolute build an absolute URI; default=false
   * @param secure 1 for force HTTPS, -1 for force HTTP, 0 for use current URI scheme; default=0
   */
  def uri[R: Manifest](method: String = null, query: Map[String, AnyRef] = Map.empty,
                       fragment: String = null, port: Int = -1, absolute: Boolean = false,
                       secure: Int = 0): URI = {
    val clazz = manifest[R].erasure
    val isSecure = secure > 0
    val isAbsolute = absolute || isSecure

    val builder = uriBuilder.path(clazz)

    // method sub path
    if (method != null) builder.path(clazz, method)

    // scheme
    if (secure != 0)
      builder scheme (if (isSecure) "https" else "http")

    // port
    builder port port

    // query parameters
    query map {
      case (name, values: Seq[AnyRef]) => builder.queryParam(name, values: _*)
      case (name, value) => builder.queryParam(name, value)
    }

    // fragment
    if (fragment != null) builder fragment fragment

    // to URI
    val uri = builder.build()

    if (isAbsolute)
      new URI(uri.getScheme, null, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)
    else
      new URI(null, null, null, -1, uri.getPath, uri.getQuery, uri.getFragment)
  }

}
