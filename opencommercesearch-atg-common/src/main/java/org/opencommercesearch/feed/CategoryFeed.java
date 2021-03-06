package org.opencommercesearch.feed;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.opencommercesearch.RulesBuilder;
import org.opencommercesearch.api.ProductService;
import org.opencommercesearch.repository.CategoryProperty;
import org.opencommercesearch.repository.RuleBasedCategoryProperty;
import org.opencommercesearch.service.localeservice.FeedLocaleService;

import atg.json.JSONArray;
import atg.json.JSONException;
import atg.json.JSONObject;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.google.common.collect.Iterables;

/**
 * A feed for categories.
 */
@SuppressWarnings("unchecked")
public class CategoryFeed extends BaseRestFeed {

    public static String[] REQUIRED_FIELDS = { "id", "name", "seoUrlToken" };
    private RulesBuilder rulesBuilder;
    private FeedLocaleService feedLocaleService;
    /**
     * Return the Endpoint for this feed
     * @return an Endpoint enum representing the endpoint for this feed
     */
    public ProductService.Endpoint getEndpoint() {
        return ProductService.Endpoint.CATEGORIES;
    }

    /**
     * Convert the given repository item to its corresponding JSON API format.
     * @param item Repository item to convert.
     * @return The JSON representation of the given repository item, or null if there are missing fields.
     * @throws atg.json.JSONException if there are format issues when creating the JSON object.
     * @throws atg.repository.RepositoryException if item data from the list can't be read.
     */
    @SuppressWarnings("unchecked")
	protected JSONObject repositoryItemToJson(RepositoryItem item) throws JSONException, RepositoryException {
        String seoUrlToken = (String)item.getPropertyValue("seoUrlToken");
        String canonicalUrl = (String)item.getPropertyValue("canonicalUrl");
        if(StringUtils.isEmpty(seoUrlToken)) {
            return null;
        }

        
        JSONObject category = new JSONObject();

        category.put("id", item.getRepositoryId());
        category.put("name", item.getItemDisplayName());
        category.put("seoUrlToken", seoUrlToken);
        category.put("canonicalUrl", canonicalUrl);
        boolean isRuleBased = RuleBasedCategoryProperty.ITEM_DESCRIPTOR.equals(item.getItemDescriptor().getItemDescriptorName());
        category.put("isRuleBased", isRuleBased);
        if(isRuleBased) {
            category.put("ruleFilters", buildRuleBasedCategory(item));
        }
        //TODO gsegura: move this calculation out of the feed into the ProductController once we provide a caching layer
        //to access products to avoid performance issues
        category.put("hierarchyTokens", buildHierarchyTokens(item));
        //TODO get rid of "catalogs", currently kept for backwards compatibility only
        Collection<RepositoryItem> catalogs = (Collection<RepositoryItem>) item.getPropertyValue("catalogs");
        setIdsProperty(category, "sites", catalogs, false);
        RepositoryItem parentCategory = (RepositoryItem) item.getPropertyValue("parentCategory");
        if (parentCategory == null && catalogs != null && !catalogs.isEmpty()) {
            RepositoryItem firstSite = catalogs.iterator().next();
            Map<String, RepositoryItem> parentCategories = (Map<String, RepositoryItem>) item.getPropertyValue("parentCategoriesForCatalog");
            if (parentCategories != null) {
                parentCategory = parentCategories.get(firstSite.getRepositoryId());
            }
        }
        setIdsProperty(category, "parentCategories", (Collection<RepositoryItem>) item.getPropertyValue("fixedParentCategories"), true, parentCategory);
        setIdsProperty(category, "childCategories", (Collection<RepositoryItem>) item.getPropertyValue("fixedChildCategories"), true);
        return category;
    }

    private JSONArray buildRuleBasedCategory(RepositoryItem item) {
        JSONArray array = new JSONArray();
        Locale[] locales = feedLocaleService.getSupportedLocales();
        if(locales != null) {
            for(Locale locale : locales) {
                array.add( locale + ":" + rulesBuilder.buildRulesFilter(item.getRepositoryId(), locale));
            }
        }
        return array;
    }

    private JSONArray buildHierarchyTokens(RepositoryItem category) {
        List<String> placeHolder = new ArrayList<String>();
        if(category != null) {
            buildHierarchyTokensAux( category, StringUtils.EMPTY, placeHolder, 0);
        }
        return new JSONArray(placeHolder);
    }

    @SuppressWarnings("unchecked")
	private void buildHierarchyTokensAux(RepositoryItem category, String groupedString, List<String> placeHolder, int depth) {
        
        Set<RepositoryItem> parentCategories = (Set<RepositoryItem>) category.getPropertyValue(CategoryProperty.FIXED_PARENT_CATEGORIES);
        
        if(parentCategories == null || parentCategories.size() == 0) {
            Set<RepositoryItem> parentCatalogs = (Set<RepositoryItem>)category.getPropertyValue(CategoryProperty.PARENT_CATALOGS);
            if(parentCatalogs != null && parentCatalogs.size() > 0) {
                placeHolder.add(depth + "." + Iterables.get(parentCatalogs, 0).getRepositoryId() + "." + groupedString);
            }
        } else {
            for(RepositoryItem parentCategory : parentCategories) {
                if(groupedString.isEmpty()) {
                    buildHierarchyTokensAux( parentCategory, category.getItemDisplayName(), placeHolder, depth+1);
                } else {
                    buildHierarchyTokensAux( parentCategory, category.getItemDisplayName()+"."+groupedString, placeHolder, depth+1);
                }
            }
        }
    }

    private void setIdsProperty(JSONObject obj, String propertyName, Collection<RepositoryItem> items, boolean asObject, RepositoryItem...firstItems) throws JSONException {
        JSONArray jsonItems = new JSONArray();
        Set<String> insertedItems = new HashSet<String>();
        for (RepositoryItem item : firstItems) {
            if (item != null) {
                addItemToJsArray(asObject, jsonItems, item, insertedItems);
            }
        }
        if (items != null) {
            for (RepositoryItem item : items) {
                if (item != null) {
                    addItemToJsArray(asObject, jsonItems, item, insertedItems);
                }
            }
        }
        if (!insertedItems.isEmpty()) {
            obj.put(propertyName, jsonItems);
        }
    }

    private void addItemToJsArray(boolean asObject, JSONArray jsonItems, RepositoryItem item, Set<String> insertedItems) throws JSONException {
        final String itemId = item.getRepositoryId();
        JSONObject itemObj = new JSONObject();
        if (insertedItems.contains(itemId)) {
            return;
        }
        if (asObject) {
            itemObj.put("id", itemId);
            jsonItems.add(itemObj);
        } else {
            jsonItems.add(item.getRepositoryId());
        }
        insertedItems.add(itemId);
    }

    /**
     * Return a list of required fields when transforming a repository item to JSON.
     * <p/>
     * This list is used for logging purposes only.
     * @return List of required fields when transforming a repository item to JSON, required for logging purposes.
     */
    protected String[] getRequiredItemFields() {
        return REQUIRED_FIELDS;
    }

    public RulesBuilder getRulesBuilder() {
        return rulesBuilder;
    }

    public void setRulesBuilder(RulesBuilder rulesBuilder) {
        this.rulesBuilder = rulesBuilder;
    }

    public FeedLocaleService getFeedLocaleService() {
        return feedLocaleService;
    }

    public void setFeedLocaleService(FeedLocaleService feedLocaleService) {
        this.feedLocaleService = feedLocaleService;
    }
    
}
