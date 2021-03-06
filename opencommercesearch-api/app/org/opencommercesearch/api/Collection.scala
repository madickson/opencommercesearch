package org.opencommercesearch.api

import play.api.Play
import play.api.i18n.Lang

import scala.language.implicitConversions

import org.opencommercesearch.common._

import Play.current

/**
 * @author rmerizalde
 */
trait Collection {

  def name(lang : Lang) : String = s"${baseName}_${lang.language}"

  protected val baseName : String
}

object Collection {
  import Implicits._

  def searchCollection(implicit context: Context) : SearchCollection = context

  object Implicits {
    implicit def contextToSearchCollection(context : Context) : SearchCollection = context.isPublic match {
      case true => PublicSearchCollection
      case false => PreviewSearchCollection
    }
  }
}

sealed abstract class SearchCollection extends Collection {}

object PublicSearchCollection extends SearchCollection {
  val baseName = Play.configuration.getString("public.collection.search").getOrElse("catalogPublic")
}

object PreviewSearchCollection extends SearchCollection {
  val baseName = Play.configuration.getString("preview.collection.search").getOrElse("catalogPreview")
}
