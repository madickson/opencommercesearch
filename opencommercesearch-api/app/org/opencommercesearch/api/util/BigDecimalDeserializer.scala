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


import com.fasterxml.jackson.databind.{SerializerProvider, DeserializationContext, JsonDeserializer}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import org.apache.commons.lang3.StringUtils

/**
 * Deserilizes a String into a BigDecimal
 */
class BigDecimalDeserializer extends JsonDeserializer[Option[BigDecimal]] {

  override  def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext) : Option[BigDecimal] = {
    val str = jsonParser.getText
    var value: Option[BigDecimal] = None

    if (StringUtils.isNotBlank(str)) {
      value = Some(BigDecimal(str))
    }
    value
  }

  override def getNullValue = None

}
