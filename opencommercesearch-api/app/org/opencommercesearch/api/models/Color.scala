package org.opencommercesearch.api.models

import play.api.libs.json.Json
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/*
* Licensed to OpenCommerceSearch under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. OpenCommerceSearch licenses this
* file to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
case class Color(
  @JsonProperty("name") var name: Option[String],
  @JsonProperty("family") var family: Option[String]) {
  @JsonCreator
  def this() = this(None, None)
}

object Color {
  implicit val readsColorInfo = Json.reads[Color]
  implicit val writesColorInfo = Json.writes[Color]
}