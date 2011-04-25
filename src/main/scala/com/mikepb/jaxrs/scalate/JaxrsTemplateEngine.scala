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

import _root_.org.fusesource.scalate.{TemplateEngine, Binding}
import _root_.org.fusesource.scalate.layout.DefaultLayoutStrategy
import _root_.org.fusesource.scalate.util.ClassPathBuilder

import _root_.scala.tools.nsc.Global

import _root_.javax.servlet.{ServletConfig, ServletContext}
import _root_.java.io.File

/**
 * JAX-RS template engine.
 *
 * Based on [[org.fusesource.scalate.servlet.ServletTemplateEngine]].
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
class JaxrsTemplateEngine(context: ServletContext)
  extends TemplateEngine {

  Option(context.getRealPath("/")) foreach {
    x: String => sourceDirectories = List(new File(x))
  }
  Option(context.getInitParameter("scalate.allowCaching")).foreach(x => allowCaching = x.toBoolean)
  Option(context.getInitParameter("scalate.allowReload")).foreach(x => allowReload = x.toBoolean)
  Option(context.getInitParameter("scalate.bootClassName")).foreach(x => bootClassName = x)

  classpath = new ClassPathBuilder()

    // servlet class path
    .addClassesDir(context.getRealPath("/WEB-INF/classes"))
    .addLibDir(context.getRealPath("/WEB-INF/lib"))

    // containers class path
    .addPathFrom(getClass)
    .addPathFrom(classOf[ServletConfig])
    .addPathFrom(classOf[Product])
    .addPathFrom(classOf[Global])

    // build class path
    .classPath

  classLoader = Thread.currentThread.getContextClassLoader
  bindings = List(Binding("context", "_root_." + classOf[JaxrsRenderContext].getName, true, isImplicit = true))
  bootInjections = List(this, context)

  templateDirectories = List("WEB-INF", "")
  layoutStrategy = new DefaultLayoutStrategy(this, {
    context.getInitParameter("scalate.defaultLayout") match {
      case null => TemplateEngine.templateTypes.map("/WEB-INF/scalate/layouts/default." + _)
      case layout => Seq(layout)
    }
  }: _*)

}
