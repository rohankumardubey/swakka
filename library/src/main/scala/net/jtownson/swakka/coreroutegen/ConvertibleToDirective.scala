/*
 * Copyright 2017 Jeremy Townson
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
package net.jtownson.swakka.coreroutegen

import akka.http.scaladsl.server.Directive1

/**
  * ConvertibleToDirective is a type class supporting the conversion
  * of parameter entities (query params, path params, etc) into Akka-Http directives
  * that extract the values of those entities.
  * These Directives are composed by RouteGen into a single Route.
  */
trait ConvertibleToDirective[T] {
  def convertToDirective(modelPath: String, t: T): Directive1[T]
}
