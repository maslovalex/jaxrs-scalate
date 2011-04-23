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

  def uriBuilder: UriBuilder = uriInfo.getBaseUriBuilder

  def uri[R: Manifest](method: Option[String], query: Map[String, String], fragment: Option[String], port: Option[Int],
                       absolute: Option[Boolean], secure: Option[Boolean]): URI = {
    val clazz = manifest[R].erasure
    val isSecure = secure.getOrElse(false)
    val isAbsolute = absolute.getOrElse(false) || isSecure

    val builder = uriBuilder.path(clazz)

    // method sub path
    method.foreach(builder.path(clazz, _))

    // scheme
    if (secure.isDefined)
      builder scheme (if (isSecure) "https" else "http")

    // port
    port.foreach(builder.port)

    // query parameters
    query.map(x => builder.queryParam(x._1, x._2))

    // fragment
    fragment.foreach(builder.fragment)

    // to URI
    val uri = builder.build()

    if (isAbsolute)
      new URI(uri.getScheme, null, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)
    else
      new URI(null, null, null, -1, uri.getPath, uri.getQuery, uri.getFragment)
  }

  // ---------------------------------------------------------- expanded

  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String, port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), query, Some(fragment), Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String, port: Int, absolute: Boolean /*            */): URI = uri[R](Some(method), query, Some(fragment), Some(port), Some(absolute), None)
  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String, port: Int /*                               */): URI = uri[R](Some(method), query, Some(fragment), Some(port), None, None)
  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String, /*      */ absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), query, Some(fragment), None, Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String, /*      */ absolute: Boolean /*            */): URI = uri[R](Some(method), query, Some(fragment), None, Some(absolute), None)
  def uri[R: Manifest](method: String, query: Map[String, String], fragment: String /*                                         */): URI = uri[R](Some(method), query, Some(fragment), None, None, None)

  def uri[R: Manifest](method: String, query: Map[String, String], /*             */ port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), query, None, Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, query: Map[String, String], /*             */ port: Int, absolute: Boolean /*            */): URI = uri[R](Some(method), query, None, Some(port), Some(absolute), None)
  def uri[R: Manifest](method: String, query: Map[String, String], /*             */ port: Int /*                               */): URI = uri[R](Some(method), query, None, Some(port), None, None)
  def uri[R: Manifest](method: String, query: Map[String, String], /*                        */ absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), query, None, None, Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, query: Map[String, String], /*                        */ absolute: Boolean /*            */): URI = uri[R](Some(method), query, None, None, Some(absolute), None)
  def uri[R: Manifest](method: String, query: Map[String, String] /*                                                           */): URI = uri[R](Some(method), query, None, None, None, None)

  def uri[R: Manifest](method: String, /*                       */ fragment: String, port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, /*                       */ fragment: String, port: Int, absolute: Boolean /*            */): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), Some(port), Some(absolute), None)
  def uri[R: Manifest](method: String, /*                       */ fragment: String, port: Int /*                               */): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), Some(port), None, None)
  def uri[R: Manifest](method: String, /*                       */ fragment: String, /*      */ absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), None, Some(absolute), Some(secure))
  def uri[R: Manifest](method: String, /*                       */ fragment: String, /*      */ absolute: Boolean /*            */): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), None, Some(absolute), None)
  def uri[R: Manifest](method: String, /*                       */ fragment: String /*                                         */): URI = uri[R](Some(method), Map.empty[String, String], Some(fragment), None, None, None)

  //  def uri[R: Manifest](method: String, /*                                         */ port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), Map.empty[String, String], None, Some(port), Some(absolute), Some(secure))
  //  def uri[R: Manifest](method: String, /*                                         */ port: Int, absolute: Boolean /*            */): URI = uri[R](Some(method), Map.empty[String, String], None, Some(port), Some(absolute), None)
  //  def uri[R: Manifest](method: String, /*                                         */ port: Int /*                               */): URI = uri[R](Some(method), Map.empty[String, String], None, Some(port), None, None)
  //  def uri[R: Manifest](method: String, /*                                                    */ absolute: Boolean, secure: Boolean): URI = uri[R](Some(method), Map.empty[String, String], None, None, Some(absolute), Some(secure))
  //  def uri[R: Manifest](method: String, /*                                                    */ absolute: Boolean /*            */): URI = uri[R](Some(method), Map.empty[String, String], None, None, Some(absolute), None)
  //  def uri[R: Manifest](method: String  /*                                                                                       */): URI = uri[R](Some(method), Map.empty[String, String], None, None, None, None)

  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String, /*            */ port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String, /*            */ port: Int, absolute: Boolean /*            */): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, Some(port), Some(absolute), None)
  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String, /*            */ port: Int /*                               */): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, Some(port), None, None)
  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String, /*                       */ absolute: Boolean, secure: Boolean): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, None, Some(absolute), Some(secure))
  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String, /*                       */ absolute: Boolean /*            */): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, None, Some(absolute), None)
  def uri[R: Manifest](isMethod: Boolean, methodOrFragment: String /*                                                          */): URI = uri[R](if (isMethod) Some(methodOrFragment) else None, Map.empty[String, String], if (!isMethod) Some(methodOrFragment) else None, None, None, None)

  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String, port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](None, query, Some(fragment), Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String, port: Int, absolute: Boolean /*            */): URI = uri[R](None, query, Some(fragment), Some(port), Some(absolute), None)
  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String, port: Int /*                               */): URI = uri[R](None, query, Some(fragment), Some(port), None, None)
  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String, /*      */ absolute: Boolean, secure: Boolean): URI = uri[R](None, query, Some(fragment), None, Some(absolute), Some(secure))
  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String, /*      */ absolute: Boolean /*            */): URI = uri[R](None, query, Some(fragment), None, Some(absolute), None)
  def uri[R: Manifest](/*           */ query: Map[String, String], fragment: String /*                                         */): URI = uri[R](None, query, Some(fragment), None, None, None)

  def uri[R: Manifest](/*           */ query: Map[String, String], /*             */ port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](None, query, None, Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](/*           */ query: Map[String, String], /*             */ port: Int, absolute: Boolean /*            */): URI = uri[R](None, query, None, Some(port), Some(absolute), None)
  def uri[R: Manifest](/*           */ query: Map[String, String], /*             */ port: Int /*                               */): URI = uri[R](None, query, None, Some(port), None, None)
  def uri[R: Manifest](/*           */ query: Map[String, String], /*                        */ absolute: Boolean, secure: Boolean): URI = uri[R](None, query, None, None, Some(absolute), Some(secure))
  def uri[R: Manifest](/*           */ query: Map[String, String], /*                        */ absolute: Boolean /*            */): URI = uri[R](None, query, None, None, Some(absolute), None)
  def uri[R: Manifest](/*           */ query: Map[String, String] /*                                                           */): URI = uri[R](None, query, None, None, None, None)

  def uri[R: Manifest](/*                                       */ fragment: String, port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](None, Map.empty[String, String], Some(fragment), Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](/*                                       */ fragment: String, port: Int, absolute: Boolean /*            */): URI = uri[R](None, Map.empty[String, String], Some(fragment), Some(port), Some(absolute), None)
  def uri[R: Manifest](/*                                       */ fragment: String, port: Int /*                               */): URI = uri[R](None, Map.empty[String, String], Some(fragment), Some(port), None, None)
  def uri[R: Manifest](/*                                       */ fragment: String, /*      */ absolute: Boolean, secure: Boolean): URI = uri[R](None, Map.empty[String, String], Some(fragment), None, Some(absolute), Some(secure))
  def uri[R: Manifest](/*                                       */ fragment: String, /*      */ absolute: Boolean /*            */): URI = uri[R](None, Map.empty[String, String], Some(fragment), None, Some(absolute), None)
  //  def uri[R: Manifest](/*                                       */ fragment: String /*                                         */): URI = uri[R](None, Map.empty[String, String], Some(fragment), None, None, None)

  def uri[R: Manifest](/*                                                         */ port: Int, absolute: Boolean, secure: Boolean): URI = uri[R](None, Map.empty[String, String], None, Some(port), Some(absolute), Some(secure))
  def uri[R: Manifest](/*                                                         */ port: Int, absolute: Boolean /*            */): URI = uri[R](None, Map.empty[String, String], None, Some(port), Some(absolute), None)
  def uri[R: Manifest](/*                                                         */ port: Int /*                               */): URI = uri[R](None, Map.empty[String, String], None, Some(port), None, None)
  def uri[R: Manifest](/*                                                                    */ absolute: Boolean, secure: Boolean): URI = uri[R](None, Map.empty[String, String], None, None, Some(absolute), Some(secure))
  def uri[R: Manifest](/*                                                                    */ absolute: Boolean /*            */): URI = uri[R](None, Map.empty[String, String], None, None, Some(absolute), None)
  def uri[R: Manifest] /*                                                                                                       */ : URI = uriBuilder.path(manifest[R].erasure).build()

}
