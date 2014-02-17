package org.opencommercesearch.api.util

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

import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer}
import com.fasterxml.jackson.core.JsonGenerator
import org.apache.commons.lang3.StringUtils

/**
 * Serializes a BigDecimal object into a String
 */
class BigDecimalSerializer extends JsonSerializer[Option[BigDecimal]] {

  override def serialize(value: Option[BigDecimal], jsonGenerator: JsonGenerator, provider: SerializerProvider) : Unit = {
    val v = value.getOrElse(null)
    jsonGenerator.writeString(if (v != null) v.toString() else StringUtils.EMPTY)
  }

}