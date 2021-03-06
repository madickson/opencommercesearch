package org.opencommercesearch.api.models

import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}

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

case class Attribute(var name: Option[String] = None, var value: Option[String] = None, var searchable: Option[Boolean] = None) {
}

object Attribute {

  implicit val readsAttribute = Json.reads[Attribute]
  implicit val writesAttribute = Json.writes[Attribute]
  
  implicit object AttributeWriter extends BSONDocumentWriter[Attribute] {
    import reactivemongo.bson._

    def write(attribute: Attribute): BSONDocument = BSONDocument(
      "name" -> attribute.name,
      "value" -> attribute.value,
      "searchable" -> attribute.searchable
    )
  }

  implicit object AttributeReader extends BSONDocumentReader[Attribute] {
    def read(doc: BSONDocument): Attribute = Attribute(
      doc.getAs[String]("name"),
      doc.getAs[String]("value"),
      doc.getAs[Boolean]("searchable")
    )
  }    

}
