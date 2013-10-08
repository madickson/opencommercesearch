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
import org.apache.solr.client.solrj.beans.Field

/**
 * Represents a single Facet
 * @param id is the system id of the Facet
 * @param name Display name of the facet
 * @param type Type of the facet (range, date, field, query, ...)
 * @param uiType Type of UI that should be used (hidden, reviews, colors, ...)
 * @param isMultiSelect Whether or not this facet supports multi select
 * @param minBuckets Min buckets (or filters) to show. If less than this number are found, no facet should be created.
 * @param isMixedSorting Whether or not the facet should use a mixed sorting approach.
 * @param minCount Min count per bucket. If less than this number documents are found for a bucket, the bucket should not be created.
 * @param sort Sort mechanism. Can be count (default), or by index - i.e. alphabetically.
 * @param isMissing Whether or not a "missing" bucket should be shown for the facet.
 * @param limit Max number of buckets to show.
 * @param start On range facets, this is the start range.
 * @param end On range facets, this is the end range.
 * @param gap On range facets, this is the gap between each bucket.
 * @param isHardened Whether or not a range facet is hardened. This means whether or not use the min possible value, or use the next "gap" when the end of the range is reached.
 * @param queries Array of queries used by query facets.
 */
case class Facet(
  var id: Option[String],
  var name: Option[String],
  var `type`: Option[String],
  var uiType: Option[String],
  var isMultiSelect: Option[Boolean],
  var minBuckets: Option[Int],
  var isMixedSorting: Option[Boolean],
  var minCount: Option[Int],
  var sort: Option[String],
  var isMissing: Option[Boolean],
  var limit: Option[Int],
  var start: Option[Int],
  var end: Option[Int],
  var gap: Option[Int],
  var isHardened: Option[Boolean],
  var queries: Option[Array[String]]) {

  def this() = this(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)

  def getId : String = { id.get }

  @Field
  def setId(id: String) : Unit = {
    this.id = Option.apply(id)
  }

  def getName : String = { name.get }

  @Field
  def setName(name: String) : Unit = {
    this.name = Option.apply(name)
  }

  def getType : String = { `type`.get }

  @Field
  def setType(`type`: String) : Unit = { this.`type`= Option.apply(`type`) }

  def getUiType : String = { uiType.getOrElse(null) }

  @Field
  def setUiType(uiType: String) : Unit = {this.uiType = Option.apply(uiType) }

  def getIsMultiSelect : java.lang.Boolean = { isMultiSelect.getOrElse(null).asInstanceOf[java.lang.Boolean] }

  @Field
  def setIsMultiSelect(isMultiSelect: Boolean) : Unit = {this.isMultiSelect = Option.apply(isMultiSelect) }

  def getMinBuckets : Integer = { minBuckets.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setMinBuckets(minBuckets: Integer) : Unit = {this.minBuckets = Option.apply(minBuckets) }

  def getIsMixedSorting : java.lang.Boolean = { isMixedSorting.getOrElse(null).asInstanceOf[java.lang.Boolean] }

  @Field
  def setIsMixedSorting(isMixedSorting: Boolean) : Unit = {this.isMixedSorting = Option.apply(isMixedSorting) }

  def getMinCount : Integer = { minCount.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setMinCount(minCount: Integer) : Unit = {this.minCount = Option.apply(minCount) }

  def getSort : String = { sort.getOrElse(null) }

  @Field
  def setSort(sort: String) : Unit = {this.sort = Option.apply(sort) }

  def getIsMissing : java.lang.Boolean = { isMissing.getOrElse(null).asInstanceOf[java.lang.Boolean] }

  @Field
  def setIsMissing(isMissing: Boolean) : Unit = {this.isMissing = Option.apply(isMissing) }

  def getLimit : Integer = { limit.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setLimit(limit: Integer) : Unit = {this.limit = Option.apply(limit) }

  def getStart : Integer = { start.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setStart(start: Integer) : Unit = {this.start = Option.apply(start) }

  def getEnd : Integer = { end.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setEnd(end: Integer) : Unit = {this.end = Option.apply(end) }

  def getGap : Integer = { gap.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setGap(gap: Integer) : Unit = {this.gap = Option.apply(gap) }

  def getIsHardened : java.lang.Boolean = { isHardened.getOrElse(null).asInstanceOf[java.lang.Boolean] }

  @Field
  def setIsHardened(isHardened: Boolean) : Unit = {this.isHardened = Option.apply(isHardened) }

  def getQueries : Array[String] = { queries.getOrElse(null) }

  @Field
  def setQueries(queries: Array[String]) : Unit = {this.queries = Option.apply(queries) }
}

object Facet {
  implicit val readsFacet = Json.reads[Facet]
  implicit val writesFacet = Json.writes[Facet]
}

/**
 * Represents a list of Facets
 *
 * @param facets are the Facets in the list
 */
case class FacetList(facets: List[Facet]) {

}

object FacetList {
  implicit val readsFacetList = Json.reads[FacetList]
  implicit val writesFacetList = Json.writes[FacetList]
}


