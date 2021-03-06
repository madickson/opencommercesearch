package org.opencommercesearch.repository;

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

public class FacetProperty {

    protected FacetProperty() {
    }

    public static final String TYPE = "facetType";
    public static final String IS_MULTI_SELECT = "isMultiSelect";
    public static final String IS_MIXED_SORTING = "isMixedSorting";
    public static final String IS_BY_COUNTRY = "isByCountry";
    public static final String IS_BY_SITE = "isBySite";
    public static final String NAME = "name";
    public static final String UI_TYPE = "uiType";
    public static final String MIN_BUCKETS = "minBuckets";
    // TODO: refactor fieldName column to cs_facet table.
    public static final String FIELD = "fieldName";
    public static final String BLACKLIST = "blacklist";
    public static final String MIN_COUNT = "minCount";
    public static final String SORT = "sort";
    public static final String MISSING = "missing";
    public static final String LIMIT = "limit";
    public static final String START = "start";
    public static final String END = "end";
    public static final String GAP = "gap";
    public static final String HARDENED = "hardened";
    public static final String QUERIES = "queries";
}
