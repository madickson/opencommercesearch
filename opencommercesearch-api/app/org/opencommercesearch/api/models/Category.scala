package org.opencommercesearch.api.models

import play.api.libs.json._
import play.api.libs.json.util._

import scala.collection.convert.Wrappers.JIterableWrapper


import java.util

import org.apache.solr.common.SolrInputDocument
import org.apache.solr.client.solrj.beans.Field
import play.api.libs.functional.syntax._
import scala.Some

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

/**
 * A category model.
 *
 * @param id is the category id
 * @param name is the category name
 * @param isRuleBased indicates if this a rule based category or not
 * @param catalogs the catalogs the categories belongs to
 * @param parentCategories the parent categories. Empty for root categories
 */
case class Category(
  var id: Option[String],
  var name: Option[String],
  var isRuleBased: Option[Boolean],
  var catalogs: Option[Seq[String]],
  var parentCategories: Option[Seq[Category]],
  var childCategories: Option[Seq[Category]]) {

  /**
   * This constructor is for lazy loaded categories
   * @param id is the id of the category to lazy load
   */
  def this(id: Option[String]) = this(id, None, None, None, None, None)

  /**
   * This constructor is intended document object binder used to load Solr documents
   */
  def this() = this(None)

  @Field
  def setId(id: String) {
    this.id = Some(id)
  }

  @Field
  def setName(name: String) {
    this.name = Some(name)
  }

  @Field("isRuleBased")
  def setRuleBased(isRuleBased: Boolean) {
    this.isRuleBased = Some(isRuleBased)
  }

  @Field("catalogs")
  def setCatalogs(catalogs: util.Collection[String]) {
    this.catalogs = Some(JIterableWrapper(catalogs).toSeq)
  }

  @Field
  def setParentCategories(parentCategories: util.Collection[String]) {
    this.parentCategories = Some(JIterableWrapper(parentCategories).toSeq.map(id => new Category(Some(id))))
  }

  @Field("childCategories")
  def setChildCategories(childCategories: util.Collection[String]) {
    this.childCategories = Some(JIterableWrapper(childCategories).toSeq.map(id => new Category(Some(id))))
  }
}

object Category {
  implicit val readsCategory : Reads[Category] = (
    (__ \ "id").readNullable[String] ~
    (__ \ "name").readNullable[String] ~
    (__ \ "isRuleBased").readNullable[Boolean] ~
    (__ \ "catalogs").readNullable[Seq[String]] ~
    (__ \ "parentCategories").lazyReadNullable(Reads.list[Category](readsCategory)) ~
    (__ \ "childCategories").lazyReadNullable(Reads.list[Category](readsCategory))
  ) (Category.apply _)

  implicit val writesCategory : Writes[Category] = (
    (__ \ "id").writeNullable[String] ~
    (__ \ "name").writeNullable[String] ~
    (__ \ "isRuleBased").writeNullable[Boolean] ~
    (__ \ "catalogs").writeNullable[Seq[String]] ~
    (__ \ "parentCategories").lazyWriteNullable(Writes.traversableWrites[Category](writesCategory)) ~
    (__ \ "childCategories").lazyWriteNullable(Writes.traversableWrites[Category](writesCategory))
  ) (unlift(Category.unapply))
}

case class CategoryList(categories: Seq[Category], feedTimestamp: Long) {
  def toDocuments() : util.List[SolrInputDocument] = {
    val documents = new util.ArrayList[SolrInputDocument](categories.size)
    var expectedDocCount = 0
    var currentDocCount = 0

    for (category: Category <- categories) {
      expectedDocCount += 1
      for (id <- category.id; name <- category.name; isRuleBased <- category.isRuleBased) {
        val doc = new SolrInputDocument()

        doc.setField("id", id)
        doc.setField("name", name)
        doc.setField("isRuleBased", isRuleBased)
        doc.setField("feedTimestamp", feedTimestamp)

        var hasCatalogs = false
        for (catalogs <- category.catalogs) {
          hasCatalogs = catalogs.size > 0
          for (catalog <- catalogs) {
            doc.addField("catalogs", catalog)
          }
        }

        var hasParents = false
        for (parentCategories <- category.parentCategories) {
          hasParents = parentCategories.size > 0
          for (parentCategory <- parentCategories) {
            for (id <- parentCategory.id) {
              doc.addField("parentCategories", id)
            }
          }
        }

        var hasChildren = false
        for (childCategories <- category.childCategories) {
          hasChildren = childCategories.size > 0
          for (childCategory <- childCategories) {
            for (id <- childCategory.id) {
              doc.addField("childCategories", id)
            }
          }
        }

        // this is just informational info
        if (hasCatalogs) {
          if (!hasParents && hasChildren) {
            doc.setField("isRoot", true)
          } else if (hasParents && hasChildren) {
            doc.setField("isNode", true)
          } else if (hasParents && !hasChildren) {
            doc.setField("isLeaf", true)
          } else {
            // shouldn't happen
            doc.setField("isOrphan", true)
          }
        } else {
          doc.setField("isOrphan", true)
        }


        documents.add(doc)
        currentDocCount += 1
      }
      if (expectedDocCount != currentDocCount) {
        throw new IllegalArgumentException("Missing required fields for category " + category.id.get)
      }
    }

    return documents
  }
}

object CategoryList {
  implicit val readsCategoryList = Json.reads[CategoryList]
}