package org.opencommercesearch.api

import play.api.mvc.Request

import org.apache.solr.client.solrj.request.AsyncUpdateRequest
import org.opencommercesearch.common.Context

/**
 * Represents a product update request
 *
 * @author rmerizalde
 */
sealed class ProductUpdate(implicit context: Context, request: Request[_]) extends AsyncUpdateRequest {
  import Collection._

  setParam("collection", searchCollection.name(context.lang))


}
