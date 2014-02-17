package org.opencommercesearch.api.models


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

import play.api.libs.json._
import com.fasterxml.jackson.annotation.JsonCreator

case class BreadCrumb(
    var fieldName: Option[String], 
    var expression: Option[String], 
    var path: Option[String]){
  
  @JsonCreator
  def this() = this(None, None, None)

  def setFieldName(fieldName: String) : Unit = {
    this.fieldName = Option.apply(fieldName)
  }
  def setExpression(expression: String) : Unit = {
    this.expression = Option.apply(expression)
  }
  def setPath(path: String) : Unit = {
    this.path = Option.apply(path)
  }
}

object BreadCrumb {
  implicit val readsCrumb = Json.reads[BreadCrumb]
  implicit val writesCrumb = Json.writes[BreadCrumb]
}