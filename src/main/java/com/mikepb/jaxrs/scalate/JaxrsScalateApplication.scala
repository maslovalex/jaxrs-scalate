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

import _root_.javax.servlet.ServletContext
import _root_.javax.ws.rs.core.{Context, Application}
import _root_.java.util.HashSet

/**
 * JAX-RS Scalate application configuration mix-in.
 *
 * @since 1.0
 * @author Michael Phan-Ba
 */
trait JaxrsScalateApplication extends Application {

  @Context
  var servletContext: ServletContext = _
  var scalateUseCache = true

  override def getSingletons: java.util.Set[AnyRef] = {
    val engine = new JaxrsTemplateEngine(servletContext); engine.boot
    val scalate = new JaxrsScalateProvider(engine, scalateUseCache)
    val providers = new HashSet[AnyRef]
    providers.add(scalate)
    providers.addAll(super.getSingletons)
    providers
  }

}
