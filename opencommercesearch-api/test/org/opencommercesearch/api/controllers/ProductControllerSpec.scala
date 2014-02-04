package org.opencommercesearch.api.controllers

import play.api.libs.json.{JsError, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication}

import scala.concurrent.{Future}

import java.util

import org.specs2.mutable.Before
import org.opencommercesearch.api.Global._
import org.apache.solr.client.solrj.{SolrQuery, AsyncSolrServer}
import org.apache.solr.client.solrj.beans.DocumentObjectBinder
import org.apache.solr.common.{SolrDocumentList, SolrDocument}
import org.opencommercesearch.api.models.{Sku, Product, Category}
import org.apache.solr.client.solrj.response.{Group, GroupCommand, GroupResponse, QueryResponse}
import org.apache.solr.common.util.NamedList
import org.opencommercesearch.api.service.{MongoStorage, MongoStorageFactory}
import com.mongodb.WriteResult

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

class ProductControllerSpec extends BaseSpec {

  trait Products extends Before {
    def before = {
      // @todo: use di
      solrServer = mock[AsyncSolrServer]
      solrServer.binder returns mock[DocumentObjectBinder]
      storageFactory = mock[MongoStorageFactory]
      val storage = mock[MongoStorage]
      storageFactory.getInstance(anyString) returns storage
      val writeResult = mock[WriteResult]
      storage.saveProduct(any) returns Future.successful(writeResult)

      val category = new Category()
      category.isRuleBased = Option(false)
      category.hierarchyTokens = Option(Seq("2.mysite.category.subcategory"))
      storage.findCategory(any, any) returns Future.successful(category)
    }
  }

  sequential

  "Product Controller" should {

    "send 404 when a product is not found"  in new Products {
      running(FakeApplication()) {
        val expectedId = "PRD1000"

        val storage = storageFactory.getInstance("namespace")
        storage.findProduct(any, any, any, any) returns Future.successful(null)
        storage.findProduct(anyString, anyString, any) returns Future.successful(null)

        val result = route(FakeRequest(GET, routes.ProductController.findById(expectedId, "mysite").url))
        validateQueryResult(result.get, NOT_FOUND, "application/json", s"Cannot find product with id [$expectedId]")
      }
    }

    "send 200 when a product is found when searching by id" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val (expectedId, expectedTitle) = ("PRD1000", "A Product")

        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        val storage = storageFactory.getInstance("namespace")
        storage.findProduct(anyString, anyString, any, any) returns Future.successful(product)
        storage.findProduct(anyString, anyString, any) returns Future.successful(product)

        val result = route(FakeRequest(GET, routes.ProductController.findById(expectedId, "mysite").url))
        validateQueryResult(result.get, OK, "application/json")

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          product.id.get must beEqualTo(expectedId)
          product.title.get must beEqualTo(expectedTitle)
          for (skus <- product.skus) {
            for (sku <- skus) {
              sku.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when searching by query" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        val storage = storageFactory.getInstance("namespace")
        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.search("term", "mysite").url))
        validateQueryResult(result.get, OK, "application/json")
        productQuery.getQuery must beEqualTo("term")
        productQuery.getBool("rule", true)
        productQuery.getBool("isRetail", true)
        productQuery.getBool("isCloseout", true)
        productQuery.getBool("onsaleUS", false)
        productQuery.get("pageType", "search")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when browsing a category" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        val storage = storageFactory.getInstance("namespace")
        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.browse("mysite", "cat1", outlet = false, preview = false).url))
        validateQueryResult(result.get, OK, "application/json")
        validateBrowseQueryParams(productQuery, "mysite", "cat1", null, isOutlet = false, escapedCategoryFilter = "2.mysite.category.subcategory")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when browsing a rule based category" in new Products {
      running(FakeApplication()) {

        val category = new Category()
        category.isRuleBased = Option(true)
        category.hierarchyTokens = Option(Seq("1.mysite.cat1"))
        category.ruleFilters = Option(Seq("en_US:(category:cat1 AND category:cat2)", "fr_CA:(category:cat1)"))
        val storage = storageFactory.getInstance("namespace")
        storage.findCategory(any, any) returns Future.successful(null)
        storage.findCategory(any, any) returns Future.successful(category)

        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.browse("mysite", "cat1", outlet = false, preview = false).url))
        validateQueryResult(result.get, OK, "application/json")
        validateBrowseQueryParams(productQuery, "mysite", "cat1", null, isOutlet = false, ruleFilter = "category:cat1 AND category:cat2", escapedCategoryFilter = "1.mysite.cat1")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when browsing an outlet category" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        val storage = storageFactory.getInstance("namespace")
        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.browse("mysite", "cat1", outlet = true, preview = false).url))
        validateQueryResult(result.get, OK, "application/json")
        validateBrowseQueryParams(productQuery, "mysite", "cat1", null, isOutlet = true, escapedCategoryFilter = "2.mysite.category.subcategory")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when browsing a brand category" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        val storage = storageFactory.getInstance("namespace")
        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.browseBrandCategory("mysite", "brand1", "cat1", preview = false).url))
        validateQueryResult(result.get, OK, "application/json")
        validateBrowseQueryParams(productQuery, "mysite", "cat1", "brand1", isOutlet = false, escapedCategoryFilter = "2.mysite.category.subcategory")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 200 when a product is found when browsing a brand" in new Products {
      running(FakeApplication()) {
        val product = new Product()
        val sku = new Sku()
        product.skus = Some(Seq(sku))
        val skuResponse = setupGroupQuery(Seq(product))
        var productQuery:SolrQuery = null

        val (expectedId, expectedTitle) = ("PRD1000", "A Product")
        product.id = Some(expectedId)
        product.title = Some(expectedTitle)

        val expectedSku = new Sku()
        expectedSku.id = Some("PRD1000-BLK-ONESIZE")
        product.skus = Some(Seq(expectedSku))

        solrServer.query(any[SolrQuery]) answers { q =>
          productQuery = q.asInstanceOf[SolrQuery]
          Future.successful(skuResponse)
        }

        val storage = storageFactory.getInstance("namespace")
        //scenario were we have only a brandId, so this api will return null
        storage.findCategory(any, any) returns Future.successful(null)
        storage.findProducts(any, any, any) returns Future.successful(Seq(product))

        val result = route(FakeRequest(GET, routes.ProductController.browseBrand("mysite", "brand1", preview = false).url))
        validateQueryResult(result.get, OK, "application/json")
        validateBrowseQueryParams(productQuery, "mysite", null, "brand1", isOutlet = false, escapedCategoryFilter = "2.mysite.category.subcategory")
        validateCommonQueryParams(productQuery)

        val json = Json.parse(contentAsString(result.get))
        (json \ "product").validate[Product].map { product =>
          for (skus <- product.skus) {
            for (sku <- skus) {
              product.id.get must beEqualTo(expectedSku.id.get)
            }
          }
        } recoverTotal {
          e => failure("Invalid JSON for product: " + JsError.toFlatJson(e))
        }
      }
    }

    "send 400 when not sending a JSON body" in new Products {
      running(FakeApplication()) {
        val (updateResponse) = setupUpdate

        val url = routes.ProductController.bulkCreateOrUpdate(preview = false).url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "text/plain"))

        val result = route(fakeRequest)
        validateFailedUpdate(updateResponse)
        validateUpdateResult(result.get, BAD_REQUEST)
      }
    }

    "send 400 when exceeding maximum products an a bulk create" in new Products {
      running(FakeApplication(additionalConfiguration = Map("product.maxUpdateBatchSize" -> 2))) {
        val (updateResponse) = setupUpdate
        val (expectedId, expectedName) = ("PRD100", "A Product")
        val jsonBrand = Json.obj("id" -> "1000")
        val json = Json.obj(
          "feedTimestamp" -> 1001,
          "products" -> Json.arr(
            Json.obj(
              "id" -> (expectedId + "0"),
              "title" -> expectedName,
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK"),
                "image" -> "/images/black.jpg",
                "isRetail" -> true,
                "isCloseout" -> false,
                "countries" -> Seq(Json.obj("code" -> "US"))
              ))
            ),
            Json.obj(
              "id" -> (expectedId + "1"),
              "title" -> (expectedName + " X"),
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "1" + "RED"),
                "image" -> "/images/red.jpg",
                "isRetail" -> true,
                "isCloseout" -> false,
                "countries" -> Seq(Json.obj("code" -> "US"))
              ))
            ),
            Json.obj(
              "id" -> (expectedId + "2"),
              "title" -> (expectedName + " Y"),
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "2" + "BLU"),
                "image" -> "/images/blue.jpg",
                "isRetail" -> true,
                "isCloseout" -> false,
                "countries" -> Seq(Json.obj("code" -> "US"))
              ))
            )
          )
        )

        val url = routes.ProductController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdate(updateResponse)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Exceeded number of products. Maximum is 2")
      }
    }


    "send 400 when trying to bulk create products with missing sku fields" in new Products {
      running(FakeApplication(additionalConfiguration = Map("product.maxUpdateBatchSize" -> 2))) {
        val (updateResponse) = setupUpdate
        val (expectedId, expectedTitle) = ("PRD0001", "A Product")
        val jsonBrand = Json.obj("id" -> "1000")
        val json = Json.obj(
          "feedTimestamp" -> 1001,
          "products" -> Json.arr(
            Json.obj(
              "id" -> expectedId,
              "title" -> expectedTitle,
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK")
              ))),
            Json.obj(
              "id" -> (expectedId + "2"),
              "title" -> expectedTitle,
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK")
              )))))

        val url = routes.ProductController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Missing required fields")
        validateFailedUpdate(updateResponse)
      }
    }

    "send 400 when trying to bulk create products with missing product fields" in new Products {
      running(FakeApplication(additionalConfiguration = Map("product.maxUpdateBatchSize" -> 2))) {
        val (updateResponse) = setupUpdate
        val (expectedId, expectedTitle) = ("PRD0001", "A Product")
        val jsonBrand = Json.obj("id" -> "1000")
        val json = Json.obj(
          "feedTimestamp" -> 1001,
          "products" -> Json.arr(
            Json.obj(
              "id" -> expectedId,
              "title" -> expectedTitle,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK")
              ))),
            Json.obj(
              "id" -> (expectedId + "2"),
              "title" -> expectedTitle,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK")
              )))))

        val url = routes.ProductController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateFailedUpdateResult(result.get, BAD_REQUEST, "Can't save an empty sku list. Check that the required fields of the products are set")
        validateFailedUpdate(updateResponse)
      }
    }

    "send 201 when a products are created" in new Products {
      running(FakeApplication()) {
        setupUpdate
        val (expectedId, expectedName) = ("PRD0001", "A Product")
        val jsonBrand = Json.obj("id" -> "PRD0001")
        val json = Json.obj(
          "feedTimestamp" -> 1001,
          "products" -> Json.arr(
            Json.obj(
              "id" -> (expectedId + "0"),
              "title" -> expectedName,
              "brand" -> jsonBrand,
              "isOutOfStock" -> true,
              "listRank" -> 1,
              "skus" -> Json.arr(Json.obj(
                "id" -> (expectedId + "0" + "BLK"),
                "image" -> "/images/black.jpg",
                "isRetail" -> true,
                "isCloseout" -> false,
                "countries" -> Seq(Json.obj("code" -> "US"))
              ))
            )))


        val url = routes.ProductController.bulkCreateOrUpdate().url
        val fakeRequest = FakeRequest(PUT, url)
          .withHeaders((CONTENT_TYPE, "application/json"))
          .withJsonBody(json)

        val result = route(fakeRequest)
        validateUpdateResult(result.get, CREATED)
      }
    }
  }

  /**
   * Helper method to validate common browse params
   * @param productQuery the query
   * @param categoryId an optional category id
   * @param brandId and optional brand id
   * @param ruleFilter and optional string to that represents the filter of a rule based category
   */
  private def validateBrowseQueryParams(productQuery: SolrQuery, site: String, categoryId: String, brandId: String,
                                        isOutlet: Boolean, ruleFilter: String = null, escapedCategoryFilter: String) : Unit = {
    var expected = 3

    productQuery.get("pageType") must beEqualTo("category")

    if (ruleFilter == null) {
      //scenario for non rule based categories
      if (categoryId != null) {
        expected += 2
        productQuery.getFilterQueries contains s"ancestorCategoryId:$categoryId"
        productQuery.getFilterQueries contains "category:" + escapedCategoryFilter
      }

      if (brandId != null) {
        expected += 1
        productQuery.getFilterQueries contains s"brand:$brandId"
      }
    } else {
      //scenario for rule based categories
      expected += 1
      productQuery.getFilterQueries contains ruleFilter
      productQuery.getBool("rulePage") must beEqualTo(true)
      productQuery.get("q") must beEqualTo("*:*")
    }
    
    if (categoryId != null) {
        productQuery.get("categoryFilter") must beEqualTo(escapedCategoryFilter)
    }
                
    productQuery.getBool("rule", true)
    productQuery.get("siteId", site)
    productQuery.getFilterQueries.size must beEqualTo(expected)
    productQuery.getFilterQueries contains "country:US"
    productQuery.getFilterQueries contains "isRetail:true"
    productQuery.getFilterQueries contains s"isOutlet:$isOutlet"
  }

  /**
   * Helper method to validate common query params
   * @param query the query
   */
  private def validateCommonQueryParams(query: SolrQuery) : Unit = {
    query.getBool("facet") must beEqualTo(true)
    query.getBool("group") must beEqualTo(true)
    query.getBool("group.ngroups") must beEqualTo(true)
    query.getInt("group.limit") must beEqualTo(50)
    query.get("group.field") must beEqualTo("productId")
    query.getBool("group.facet") must beEqualTo(false)
    query.get("group.sort") must beEqualTo("isCloseout asc, score desc, sort asc")
    query.getBool("groupcollapse") must beEqualTo(true)
    query.get("groupcollapse.fl") must beEqualTo("listPriceUS,salePriceUS,discountPercentUS")
    query.get("groupcollapse.ff") must beEqualTo("isCloseout")
    val facetFields = query.getFacetFields
    facetFields.size must beEqualTo(1)
    facetFields(0) must beEqualTo("category")
  }

  /**
   * Helper method to mock the response for findById calls
   * @param product the product use to mock the response
   * @return a query response mock
   */
  protected def setupProductQuery(product: Product) = {
    val queryResponse = mock[QueryResponse]
    val namedList = mock[NamedList[AnyRef]]

    queryResponse.getResponse returns namedList
    solrServer.query(any[SolrQuery]) returns Future.successful(queryResponse)

    val doc = mock[SolrDocument]
    namedList.get("doc") returns doc
    solrServer.binder.getBean(classOf[Product], doc) returns product

    queryResponse
  }

  /**
   * Helper method to mock the response for findById calls
   *  @param product the product & skus use to mock the response
   * @return a query response mock
   */
  protected def setupSkuQuery(product: Product) : QueryResponse = {
    val queryResponse = mock[QueryResponse]
    val documentList = new SolrDocumentList

    queryResponse.getResults returns documentList
    solrServer.query(any[SolrQuery]) returns Future.successful(queryResponse)

    if (product != null) {
      for (skus <- product.skus) {
        for (sku <- skus) {
          val doc = mock[SolrDocument]
          documentList.add(doc)

          val prod = mock[Product]
          solrServer.binder.getBean(classOf[Product], documentList.get(0)) returns prod
          solrServer.binder.getBean(classOf[Sku], documentList.get(0)) returns sku
          prod.skus returns Some(Seq(sku))
        }
      }
    }
    queryResponse
  }

  /**
   * Helper method to mock a group response
   * @param products is the list of products used to build the mock response
   * @return
   */
  private def setupGroupQuery(products: Seq[Product]) = {
    val queryResponse = mock[QueryResponse]

    solrServer.query(any[SolrQuery]) returns Future.successful(queryResponse)

    val groupCommand = mock[GroupCommand]
    val commandValues = spy(new util.ArrayList[GroupCommand])

    commandValues.add(groupCommand)
    groupCommand.getName returns "productId"
    groupCommand.getNGroups returns products.size

    val groupValues = spy(new util.ArrayList[Group](products.size))
    groupCommand.getValues returns groupValues

    for (product <- products) {
      val group = mock[Group]
      val documentList = new SolrDocumentList

      for (skus <- product.skus) {
        val skuList = new util.ArrayList[Sku](skus.size)
        for (sku <- skus) {
          documentList.add(mock[SolrDocument])
          skuList.add(sku)
        }
      }
      group.getResult returns documentList
      groupValues.add(group)
    }

    val summary = mock[NamedList[Object]]
    val groupSummary = mock[NamedList[Object]]
    summary.get("groups_summary").asInstanceOf[NamedList[Object]] returns groupSummary;

    groupSummary.get("productId").asInstanceOf[NamedList[Object]] returns null;

    val groupResponse = mock[GroupResponse]

    groupResponse.getValues returns commandValues
    queryResponse.getGroupResponse returns groupResponse
    queryResponse.getResponse returns summary;
    queryResponse
  }
}
