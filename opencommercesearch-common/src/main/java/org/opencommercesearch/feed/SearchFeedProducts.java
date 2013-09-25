package org.opencommercesearch.feed;

import org.opencommercesearch.model.Product;

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

import java.util.*;

public class SearchFeedProducts {
    private Map<Locale, List<Product>> productsByLocale = new HashMap<Locale, List<Product>>();

    public void add(Locale locale, Product product) {
        List<Product> productList = productsByLocale.get(locale);

        if (productList == null) {
            productList = new ArrayList<Product>();
            productsByLocale.put(locale, productList);
        }

        productList.add(product);
    }

    public Set<Locale> getLocales() {
        return productsByLocale.keySet();
    }

    public List<Product> getProducts(Locale locale) {
        return productsByLocale.get(locale);
    }

    public int getSkuCount(Locale locale) {
        int count = 0;
        for (Product p : productsByLocale.get(locale)) {
            if (p.getSkus() != null) {
                count += p.getSkus().size();
            }
        }
        return count;
    }

    public void clear() {
        for (Locale locale : getLocales()) {
            productsByLocale.get(locale).clear();
        }
    }
}