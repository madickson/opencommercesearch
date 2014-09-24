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

import play.api.libs.json.Json

import java.util.Date


import org.opencommercesearch.api.Implicits._
import org.opencommercesearch.api.models.Availability._

import com.fasterxml.jackson.annotation.JsonCreator

case class Availability(
  var status: Option[String] = None,
  var stockLevel: Option[Int] = None,
  var backorderLevel: Option[Int] = None,
  var date: Option[Date] = None) {

  require((status, stockLevel) match {
    case (Some(s), Some(l)) => s match {
      case OutOfStock | Backorderable | PermanentlyOutOfStock => l == 0
      case InStock => l > 0
      case _ => false
    }
    case (Some(s), _) => s match {
      case InStock | OutOfStock | PermanentlyOutOfStock | Backorderable | Preorderable => true
      case _ => false
    }
    case _ => true
  })

  require(backorderLevel match {
    case Some(l) => l >= InfiniteStock
    case None => true
  })
}

object Availability {
  val InStock = "InStock"
  val OutOfStock = "OutOfStock"
  val PermanentlyOutOfStock = "PermanentlyOutOfStock"
  val Backorderable = "Backorderable"
  val Preorderable = "Preorderable"

  val InfiniteStock = -1

  @JsonCreator
  def getInstance() = new Availability()

  implicit val readsAvailability = Json.reads[Availability]
  implicit val writesAvailability = Json.writes[Availability]
}


