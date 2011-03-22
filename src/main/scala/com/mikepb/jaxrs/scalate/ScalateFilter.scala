/*
 * Copyright 2011 Michael Phan-Ba
 */

package com.mikepb.jaxrs.scalate

import _root_.java.io.PrintWriter
import _root_.javax.servlet._
import _root_.javax.servlet.http._
import _root_.org.fusesource.scalate.servlet.{ServletRenderContext, TemplateEngineFilter}

/**
 * Helper for the Scalate JAX-RS provider.
 *
 * To use the Scalate JAX-RS provider, this Servlet filter must be
 * configured to run before the JAX-RS provider filter, if any.
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
class ScalateFilter extends TemplateEngineFilter {

  import ScalateProvider.{_context, _engine}

  /**
   * Intercepts HTTP requests to capture the servlet context for use by
   * the Scalate JAX-RS provider.
   *
   * @see [[javax.servlet.ScalateFilter.doFilter]]
   * @since 1.0
   */
  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    (request, response) match {
      case (request: HttpServletRequest, response: HttpServletResponse) =>
        _context set {
          (out: PrintWriter) =>
            new ServletRenderContext(engine, out, request, response, config.getServletContext)
        }
        _engine set engine
      case _ =>
    }
    chain.doFilter(request, response)
  }
}
