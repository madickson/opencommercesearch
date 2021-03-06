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

import com.wordnik.swagger.converter.{BaseConverter, ModelConverter, SwaggerSchemaConverter}
import com.wordnik.swagger.model.Model

/**
 * This converter prevents BigDecimal model properties from been rendered in the model schema.
 *
 * @author rmerizalde
 */
class BigDecimalConverter extends ModelConverter with BaseConverter {

  def read(cls: Class[_], typeMap: Map[String, String]): Option[Model] = None

  override def ignoredClasses: Set[String] = Set("scala.math.BigDecimal", "java.math.BigDecimal")

}
