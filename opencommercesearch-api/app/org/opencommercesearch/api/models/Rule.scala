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

import java.util.Date

import org.apache.solr.client.solrj.beans.Field
import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}

/**
 * Represents a single Rule
 *
 * @param id is the system id of the Rule
 * @param name is the system name of the Rule
 * @param query is the query for the Rule
 * @param sortPriority is the sort priority for the Rule (i.e. what order should this rule have)
 * @param combineMode is the combine mode for the Rule (replace or append)
 * @param startDate is the date this rule will start applying
 * @param endDate is the date this rule will stop applying
 * @param target is a list of target page types to which the rules will apply to
 * @param subTarget retail or outlet pages to which the rules will apply to
 * @param siteId is a list of target siteId to which the rules will apply to
 * @param catalogId is a list of catalogId to which the rules will apply to (you may have more than one catalog on per site)
 * @param category is a list of category and tokens to which the rules should apply.
 * @param boostFunction is a boost function to be applied by this rule
 * @param facetField list of facets to be created by this rule
 * @param facetId list of facet ids to be created by this rule
 * @param ruleType type of rule
 */
case class Rule(
  var id: Option[String] = None,
  var name: Option[String] = None,
  var query: Option[String] = None,
  var sortPriority: Option[Int] = None,
  var combineMode: Option[String] = None,
  var startDate: Option[String] = None,
  var endDate: Option[String] = None,
  var target: Option[Array[String]] = None,
  var subTarget: Option[Array[String]] = None,
  var siteId: Option[Array[String]] = None,
  var catalogId: Option[Array[String]] = None,
  var category: Option[Array[String]] = None,
  var brandId: Option[Array[String]] = None,
  var experimental: Option[Boolean] = None,
  var boostFunction: Option[String] = None,
  var facetField: Option[Array[String]] = None,
  var facetId: Option[Array[String]] = None,
  var boostedProducts: Option[Array[String]] = None,
  var blockedProducts: Option[Array[String]] = None,
  var ruleType: Option[String] = None,
  var redirectUrl: Option[String] = None) {

  def this() = this(id = None)

  def getId : String = { id.get }

  @Field
  def setId(id: String) : Unit = {
    this.id = Option.apply(id)
  }

  def getQuery : String = { query.get }

  @Field
  def setQuery(query: String) : Unit = { this.query = Option.apply(query) }

  def getSortPriority : Integer = { sortPriority.getOrElse(null).asInstanceOf[Integer] }

  @Field
  def setSortPriority(sortPriority: Int) : Unit = { this.sortPriority = Option.apply(sortPriority) }

  def getCombineMode : String = { combineMode.orNull }

  @Field
  def setCombineMode(combineMode: String) : Unit = { this.combineMode = Option.apply(combineMode) }

  def getStartDate : String = { startDate.orNull }

  @Field
  def setStartDate(startDate: Date) : Unit = { this.startDate = Option.apply(startDate.toString) }

  def getEndDate : String = { endDate.get }

  @Field
  def setEndDate(endDate: Date) : Unit = { this.endDate = Option.apply(endDate.toString) }

  def getTarget : Array[String] = { target.get }

  @Field
  def setTarget(target: Array[String]) : Unit = { this.target = Option.apply(target) }

  def getSubTarget : Array[String] = { subTarget.get }
  
  @Field
  def setSubTarget(subTarget: Array[String]) : Unit = { this.subTarget = Option.apply(subTarget) }
  
  def getSiteId : Array[String] = { siteId.get }

  @Field
  def setSiteId(siteId: Array[String]) : Unit = { this.siteId = Option.apply(siteId) }

  def getCatalogId : Array[String] = { catalogId.get }

  @Field
  def setCatalogId(catalogId: Array[String]) : Unit = { this.catalogId = Option.apply(catalogId) }

  def getCategory : Array[String] = { category.get }

  @Field
  def setCategory(category: Array[String]) : Unit = { this.category = Option.apply(category) }

  def getBrandId : Array[String] = { brandId.get }

  @Field
  def setBrandId(brandId: Array[String]) : Unit = { this.brandId = Option.apply(brandId) }

  def getExperimental : Boolean = { experimental.getOrElse(false) }

  @Field
  def setExperimental(experimental: Boolean) : Unit = { this.experimental = Option.apply(experimental) }

  def getBoostFunction : String = { boostFunction.orNull }

  @Field
  def setBoostFunction(boostFunction: String) : Unit = { this.boostFunction = Option.apply(boostFunction) }

  def getFacetField : Array[String] = { facetField.orNull }

  @Field
  def setFacetField(facetField: Array[String]) : Unit = { this.facetField = Option.apply(facetField) }

  def getFacetId : Array[String] = { facetId.orNull }

  @Field
  def setFacetId(facetId: Array[String]) : Unit = { this.facetId = Option.apply(facetId) }

  def getBoostedProducts : Array[String] = { boostedProducts.orNull }

  @Field
  def setBoostedProducts(boostedProducts: Array[String]) : Unit = { this.boostedProducts = Option.apply(boostedProducts) }

  def getBlockedProducts : Array[String] = { blockedProducts.orNull }

  @Field
  def setBlockedProducts(blockedProducts: Array[String]) : Unit = { this.blockedProducts = Option.apply(blockedProducts) }

  def getRuleType : String = { ruleType.orNull }

  @Field
  def setRuleType(ruleType: String) : Unit = { this.ruleType = Option.apply(ruleType) }

  def getUrl : String = { redirectUrl.orNull }

  @Field("redirectUrl")
  def setUrl(url: String) : Unit = { this.redirectUrl = Option.apply(url) }
}

/*
  var id: Option[String] = None,
  var name: Option[String] = None,
  var query: Option[String] = None,
  var sortPriority: Option[Int] = None,
  var combineMode: Option[String] = None,
  var startDate: Option[String] = None,
  var endDate: Option[String] = None,
  var target: Option[Array[String]] = None,
  var subTarget: Option[Array[String]] = None,
  var siteId: Option[Array[String]] = None,
  var catalogId: Option[Array[String]] = None,
  var category: Option[Array[String]] = None,
  var brandId: Option[Array[String]] = None,
  var experimental: Option[Boolean] = None,
  var boostFunction: Option[String] = None,
  var facetField: Option[Array[String]] = None,
  var facetId: Option[Array[String]] = None,
  var boostedProducts: Option[Array[String]] = None,
  var blockedProducts: Option[Array[String]] = None,
  var ruleType: Option[String] = None,
  var redirectUrl: Option[String] = None
 */
object Rule {
  implicit val readsRule = Json.reads[Rule]
  implicit val writesRule = Json.writes[Rule]

  implicit object RuleWriter extends BSONDocumentWriter[Rule] {
    import reactivemongo.bson._

    def write(rule: Rule): BSONDocument = BSONDocument(
      "_id" -> rule.id,
      "name" -> rule.name,
      "query" -> rule.query,
      "sortPriority" -> rule.sortPriority,
      "combineMode" -> rule.combineMode,
      "startDate" -> rule.startDate,
      "endDate" -> rule.endDate,
      "target" -> rule.target,
      "subTarget" -> rule.subTarget,
      "siteId" -> rule.siteId,
      "catalogId" -> rule.catalogId,
      "category" -> rule.category,
      "brandId" -> rule.brandId,
      "experimental" -> rule.experimental,
      "boostFunction" -> rule.boostFunction,
      "facetField" -> rule.facetField,
      "facetId" -> rule.facetId,
      "boostedProducts" -> rule.boostedProducts,
      "blockedProducts" -> rule.blockedProducts,
      "ruleType" -> rule.ruleType,
      "redirectUrl" -> rule.redirectUrl
    )
  }

  implicit object RuleReader extends BSONDocumentReader[Rule] {
    def read(doc: BSONDocument): Rule = Rule(
      doc.getAs[String]("_id"),
      doc.getAs[String]("name"),
      doc.getAs[String]("query"),
      doc.getAs[Int]("sortPriority"),
      doc.getAs[String]("combineMode"),
      doc.getAs[String]("startDate"),
      doc.getAs[String]("endDate"),
      doc.getAs[Array[String]]("target"),
      doc.getAs[Array[String]]("subTarget"),
      doc.getAs[Array[String]]("siteId"),
      doc.getAs[Array[String]]("catalogId"),
      doc.getAs[Array[String]]("category"),
      doc.getAs[Array[String]]("brandId"),
      doc.getAs[Boolean]("experimental"),
      doc.getAs[String]("boostFunction"),
      doc.getAs[Array[String]]("facetField"),
      doc.getAs[Array[String]]("facetId"),
      doc.getAs[Array[String]]("boostedProducts"),
      doc.getAs[Array[String]]("blockedProducts"),
      doc.getAs[String]("ruleType"),
      doc.getAs[String]("redirectUrl")
    )
  }
}

/**
 * Represents a list of Rules
 *
 * @param rules are the Rules in the list
 */
case class RuleList(rules: List[Rule]) {

}

object RuleList {
  implicit val readsRuleList = Json.reads[RuleList]
  implicit val writesRuleList = Json.writes[RuleList]
}



