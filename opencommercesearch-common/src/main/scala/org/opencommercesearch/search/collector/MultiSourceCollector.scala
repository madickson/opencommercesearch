package org.opencommercesearch.search.collector

/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import org.opencommercesearch.search.Element
import scala.collection.mutable

/**
 * Base implementation for a multi-source collector
 *
 * @author rmerizalde
 */
class MultiSourceCollector[E <: Element] extends MultiCollector[E] {

  val map = new mutable.HashMap[String, Collector[E]]

  override def canStop : Boolean = false

  override def isEmpty  : Boolean = collectors.collectFirst({ case c if !c.isEmpty => false }).getOrElse(true)

  override def add(element: E, source: String) : Boolean = {
    val collector = map.get(source)
    for (c <- collector) {
      c.add(element, source)
    }
    collector.isDefined
  }

  override def elements() : Seq[E] = collectors.foldLeft(new mutable.ArrayBuffer[E](size()))(_ ++= _.elements())

  override def size() : Int = collectors.foldLeft(0)(_ + _.size)

  override def capacity() : Int = collectors.foldLeft(0)(_ + _.capacity)

  override def collector(source: String): Option[Collector[E]] = map.get(source)

  override def collectors: Iterable[Collector[E]] = map.values

  override def sources: Iterable[String] = map.keySet

  override def add(source: String, collector: Collector[E]): Boolean = {
    if (source == null || collector == null || collector == this) {
      false
    } else {
      map.put(source, collector)
      true
    }
  }

}
