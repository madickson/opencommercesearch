package org.opencommercesearch;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.opencommercesearch.RulesTestUtil.mockRule;

import java.sql.Timestamp;
import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.opencommercesearch.repository.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryItemDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class RuleManagerTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    private static final String facetRule = "facetRule", blockRule = "blockRule", boostRule = "boostRule";
    
    @Mock private Repository repository;    
    @Mock private SolrServer server;
    @Mock private QueryResponse queryResponse;
    
    @Mock private RepositoryItem facetRuleItem1;
    @Mock private RepositoryItem facetRuleItem2;
    @Mock private RepositoryItem facetRuleItem3;
    @Mock private RepositoryItem blockRuleItem1;    
    @Mock private RepositoryItem boostRuleItem1;
    @Mock private RepositoryItem boostRuleItem2;
    @Mock private RepositoryItem testRuleItem;
    
    @Mock private RepositoryItem siteA, siteB, siteC;
    @Mock private RepositoryItem cataA, cataB, cataC;
    @Mock private RepositoryItem cateA, cateB, cateC, cateCchild1, cateCchild2, cateCchild3;
    @Mock private RepositoryItem cateCchild1child1, cateCchild1child2, cateCchild1child3;
    
    @Mock private RepositoryItem cateAToken1, cateAToken2;
    @Mock private RepositoryItem cateBToken;
    @Mock private RepositoryItem cateCchild1Token, cateCchild2Token;
    
    @Mock private RepositoryItemDescriptor cateDescriptor, faultyDescriptor;
    
    private static final String EXPECTED_WILDCARD = "__all__";
    
    private RulesBuilder builder = new RulesBuilder();

    @Before
    public void setup() throws Exception {
        when(testRuleItem.getPropertyValue(RuleProperty.RULE_TYPE)).thenReturn("someType");
        when(testRuleItem.getPropertyValue(RuleProperty.TARGET)).thenReturn("someTarget");
    }
    
    @Before
    public void setUpSitesForCreateRuleDocument() {
        when(siteA.getRepositoryId()).thenReturn("site:alpha");
        when(siteB.getRepositoryId()).thenReturn("site:beta");
        when(siteC.getRepositoryId()).thenReturn("site:charlie");
    }
    
    @Before
    public void setUpCatalogsForCreateRuleDocument() {
        when(cataA.getRepositoryId()).thenReturn("cata:alpha");
        Set<String> siteSet = new HashSet<String>();
        siteSet.add("site:alpha");
        when(cataA.getPropertyValue("siteIds")).thenReturn(siteSet);
        
        when(cataB.getRepositoryId()).thenReturn("cata:beta");
        siteSet = new HashSet<String>();
        siteSet.add("site:beta");
        when(cataB.getPropertyValue("siteIds")).thenReturn(siteSet);
        
        when(cataC.getRepositoryId()).thenReturn("cata:charlie");
        siteSet = new HashSet<String>();
        siteSet.add("site:charlie");
        when(cataC.getPropertyValue("siteIds")).thenReturn(siteSet);
        
    }
    
    @Before
    public void setUpCategoriesForCreateRuleDocument() throws RepositoryException {
        // cateA has 2 search tokens
        when(cateA.getRepositoryId()).thenReturn("cate:alpha");
        when(cateA.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateA:token1", "cateA:token2", })));
        // cateB has 1 search token
        when(cateB.getRepositoryId()).thenReturn("cate:beta");
        when(cateB.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateB:token", })));
        // cateC has 0 search tokens
        when(cateC.getRepositoryId()).thenReturn("cate:charlie");
        // cateC has 2 children categories however
        when(cateC.getPropertyValue(CategoryProperty.CHILD_CATEGORIES)).thenReturn(new LinkedList<RepositoryItem>(Arrays.asList(new RepositoryItem[]{ cateCchild1, cateCchild2, cateCchild3, })));
        
        // cateCchildx search tokens...
        when(cateCchild1.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild1:token", })));
        when(cateCchild2.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild2:token", })));
        when(cateCchild3.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild3:token:INVISIBLE!!!!", })));
        
        // cateCchild1childx search tokens...
        when(cateCchild1child1.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild1.cateCchild1child1:token", })));
        when(cateCchild1child2.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild1.cateCchild1child2:token", })));
        when(cateCchild1child3.getPropertyValue(CategoryProperty.SEARCH_TOKENS)).thenReturn(new HashSet<String>(Arrays.asList(new String[]{ "cateCchild1.cateCchild1child3:token", })));
        
        // set up a descriptor for all of the category search tokens
        when(cateDescriptor.getItemDescriptorName()).thenReturn("category");
        for (RepositoryItem r : new RepositoryItem[] {cateA, cateB, cateC, cateCchild1, cateCchild2, }) {
            when(r.getItemDescriptor()).thenReturn(cateDescriptor);
        }      
        // make the INVISIBLE version... it won't get into the output
        when(faultyDescriptor.getItemDescriptorName()).thenReturn("notcategory");
        when(cateCchild3.getItemDescriptor()).thenReturn(faultyDescriptor);
    }
    
    private void setUpRuleData(List<String> categories, String description, String id, String ruleType, RepositoryItem item, Boolean experimental, SolrDocumentList documents) throws RepositoryException {
        SolrDocument rule = new SolrDocument();
        rule.addField("description", description);
        rule.addField("id", id);
        rule.addField("category", categories);
        rule.addField("experimental", experimental);
        documents.add(rule);        
        when(item.getPropertyValue(RuleProperty.RULE_TYPE)).thenReturn(ruleType);
        when(item.getPropertyValue(RuleProperty.ID)).thenReturn(id);
        when(item.getRepositoryId()).thenReturn(id);
        when(repository.getItem(id, SearchRepositoryItemDescriptor.RULE)).thenReturn(item);
    }
    
    @Test 
    public void testSetRuleParamsAndSetFilterQueries() throws RepositoryException, SolrServerException {
        // make sure that the facetManager gets addFacet called when we supply facets
        final FacetManager facetManager = mock(FacetManager.class);        
        RuleManager mgr = new RuleManager(repository, builder, server) {
            @Override
            public FacetManager getFacetManager() {
                return facetManager;
            }
        };
                
        // we need to make sure that we test filterQueries here...
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        // ---------- set up docs with a rule type -----------
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        // note that we do NOT add this into the Repository so that we have a null rule in loadRules, this causes this document to not go into the rules
        SolrDocument rule = new SolrDocument();
        rule.addField("description", "description facetRule2");
        rule.addField("id", "facetRule2");
        solrDocumentList.add(rule);    
        setUpRuleData(null, "description facetRule3", "facetRule3", boostRule, boostRuleItem1, false, solrDocumentList);
                       
        // ----------- set up doclist attributes ----------
        solrDocumentList.setNumFound(solrDocumentList.size()); 
        solrDocumentList.setStart(0L);
//        solrDocumentList.setMaxScore(1000.0);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ------------ make the call to load the rules etc -------------
        RepositoryItem catalog = mock(RepositoryItem.class);
        when(catalog.getRepositoryId()).thenReturn("bobcatalog");
        SolrQuery query = mock(SolrQuery.class);
        when(query.getQuery()).thenReturn("jackets");
        
        FilterQuery[] filterQueries = new FilterQuery[] {
            new FilterQuery("category", "jackets"), // is a multi
            new FilterQuery("category", "12.jackets"), // is a multi
            new FilterQuery("hasPinStripes", "redstripes"), 
            new FilterQuery("hasFeathers", "socks&stuff"), 
            new FilterQuery("hasLaces", "raingear"), // is a multi
            new FilterQuery("chopsticks", "lookout below")
        };
        
        // set up the facet items to catch all conditions
        RepositoryItem categoryFacetItem = mock(RepositoryItem.class);
        when(facetManager.getFacetItem("category")).thenReturn(categoryFacetItem);
        when(categoryFacetItem.getPropertyValue((FacetProperty.IS_MULTI_SELECT))).thenReturn(true);
        
        RepositoryItem hasPinStripesFacetItem = mock(RepositoryItem.class);
        when(facetManager.getFacetItem("hasPinStripes")).thenReturn(hasPinStripesFacetItem);
        when(hasPinStripesFacetItem.getPropertyValue((FacetProperty.IS_MULTI_SELECT))).thenReturn(false);
        
        RepositoryItem hasFeathersFacetItem = mock(RepositoryItem.class);
        when(facetManager.getFacetItem("hasFeathers")).thenReturn(hasFeathersFacetItem);
        // don't support multi for hasFeathers...
        
        RepositoryItem hasLacesFacetItem = mock(RepositoryItem.class);
        when(facetManager.getFacetItem("hasLaces")).thenReturn(hasLacesFacetItem);
        when(hasLacesFacetItem.getPropertyValue((FacetProperty.IS_MULTI_SELECT))).thenReturn(true);        
        
        // and nothing for chopsticks
        mgr.setRuleParams(query, true, false, null, filterQueries, catalog, false, null);
        
        verify(query).setFacetPrefix("category", "1.bobcatalog.");
        verify(query).addFilterQuery("category:0.bobcatalog");
        verify(query).getQuery();
        verify(query, times(2)).getSortFields();
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
        verify(query).addSortField("_version_", ORDER.desc);
        verify(query).setFacetPrefix("category", "13.jackets.");

        // verify the single calls to addFilterQuery
        verify(query).addFilterQuery("hasPinStripes:redstripes"); // this will have a facet
        verify(query).addFilterQuery("hasFeathers:socks&stuff"); // this will have a facet, but not MULTI
        verify(query).addFilterQuery("chopsticks:lookout below"); // no facet for this one (test null path)
        
        // now verify the multi calls to addFilterQuery
        verify(query).addFilterQuery("{!tag=category}category:jackets OR category:12.jackets");
        verify(query).addFilterQuery("{!tag=hasLaces}hasLaces:raingear");        
        verify(query).getParams("excludeRules");
        verify(query).getParams("includeRules");
        verifyNoMoreInteractions(query);        
    }
    
    @Test 
    public void testSetRuleParams2NullRules() throws RepositoryException, SolrServerException { 
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        SolrDocument rule = new SolrDocument();
        rule.addField("description", "description facetRule2");
        rule.addField("id", "facetRule2");
        solrDocumentList.add(rule);    
        setUpRuleData(null, "description boostRule3", "boostRule3", boostRule, boostRuleItem1, false, solrDocumentList);

        solrDocumentList.setNumFound(solrDocumentList.size()); 
        solrDocumentList.setStart(0L);
        
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
                
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        RepositoryItem catalog = mock(RepositoryItem.class);
        when(catalog.getRepositoryId()).thenReturn("bobcatalog");
        SolrQuery query = mock(SolrQuery.class);
        when(query.getQuery()).thenReturn("jackets");
        
        mgr.setRuleParams(query, true, false, null, null, catalog, false, null);
        verify(query).setFacetPrefix("category", "1.bobcatalog.");
        verify(query).addFilterQuery("category:0.bobcatalog");
        verify(query).getQuery();
        verify(query, times(2)).getSortFields();
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
        verify(query).addSortField("_version_", ORDER.desc);
        verify(query).getParams("includeRules");
        verify(query).getParams("excludeRules");
        verifyNoMoreInteractions(query);
    }
    
    @Test
    public void testSetRuleParamsBlocks() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();
        
        List<RepositoryItem> rules = new ArrayList<RepositoryItem>();
        typeToRules.put(blockRule, rules);
        RepositoryItem rule = mock(RepositoryItem.class);
        rules.add(rule);
        Set<RepositoryItem> blockedProducts = new HashSet<RepositoryItem>();        
        when(rule.getPropertyValue(BlockRuleProperty.BLOCKED_PRODUCTS)).thenReturn(blockedProducts);
        
        RepositoryItem blockedProduct1 = mock(RepositoryItem.class);
        blockedProducts.add(blockedProduct1);
        when(blockedProduct1.getRepositoryId()).thenReturn("blockedProduct1");
        
        RepositoryItem blockedProduct2 = mock(RepositoryItem.class);
        blockedProducts.add(blockedProduct2);
        when(blockedProduct2.getRepositoryId()).thenReturn("blockedProduct2");
        
        mgr.setRuleParams(query, typeToRules);
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addFilterQuery("-productId:blockedProduct1");
        verify(query).addFilterQuery("-productId:blockedProduct2");
        verify(query).addSortField("score", ORDER.desc);        
    }  
    
    @Test
    public void testSetRuleParamsBoosts() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();

        List<RepositoryItem> rules = new ArrayList<RepositoryItem>();
        typeToRules.put(boostRule, rules);
        RepositoryItem rule = mock(RepositoryItem.class);
        rules.add(rule);
        List<RepositoryItem> boostedProducts = new ArrayList<RepositoryItem>();        
        when(rule.getPropertyValue(BoostRuleProperty.BOOSTED_PRODUCTS)).thenReturn(boostedProducts);
        
        RepositoryItem boostedProduct1 = mock(RepositoryItem.class);
        boostedProducts.add(boostedProduct1);
        when(boostedProduct1.getRepositoryId()).thenReturn("boostedProduct1");
        
        RepositoryItem boostedProduct2 = mock(RepositoryItem.class);
        boostedProducts.add(boostedProduct2);
        when(boostedProduct2.getRepositoryId()).thenReturn("boostedProduct2");
        
        mgr.setRuleParams(query, typeToRules);
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("fixedBoost(productId,'boostedProduct1','boostedProduct2')", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
    }  
    
    
    @Test
    public void testSetRuleParamsFacets() {
        // make sure that the facetManager gets addFacet called when we supply facets
        final FacetManager facetManager = mock(FacetManager.class);        
        RuleManager mgr = new RuleManager(repository, builder, server) {
            @Override
            public FacetManager getFacetManager() {
                return facetManager;
            }
        };
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();
        List<RepositoryItem> rules = new ArrayList<RepositoryItem>();
        typeToRules.put(facetRule, rules);
        RepositoryItem rule = mock(RepositoryItem.class);
        rules.add(rule);
        List<RepositoryItem> facets = new ArrayList<RepositoryItem>();
        when(rule.getRepositoryId()).thenReturn("facetRule1");
        when(rule.getPropertyValue(FacetRuleProperty.FACETS)).thenReturn(facets);
                
        RepositoryItem facet1 = mock(RepositoryItem.class);
        facets.add(facet1);
        when(facet1.getRepositoryId()).thenReturn("facet1");
        
        RepositoryItem facet2 = mock(RepositoryItem.class);
        facets.add(facet2);
        when(facet2.getRepositoryId()).thenReturn("facet2");

        Map<String, SolrDocument> ruleDocs = new HashMap<String, SolrDocument>();
        SolrDocument facetRuleDoc = mock(SolrDocument.class);
        when(facetRuleDoc.getFieldValue(FacetRuleProperty.COMBINE_MODE)).thenReturn(FacetRuleProperty.COMBINE_MODE_REPLACE);
        ruleDocs.put("facetRule1", facetRuleDoc);

        
        mgr.setRuleParams(query, typeToRules, ruleDocs);
        verify(facetManager).addFacet(facet1);
        verify(facetManager).addFacet(facet2);
        verify(facetManager).setParams(query);
        verify(facetManager).clear();
        verifyNoMoreInteractions(facetManager);     
    }
    
    // TODO: make a good test for testSetRuleParamsFacets
    
    @Test
    public void testSetRuleParamsTypesHaveNoContent() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();

        List<RepositoryItem> facetRules = new ArrayList<RepositoryItem>();
        typeToRules.put(facetRule, facetRules);
        RepositoryItem frule1 = mock(RepositoryItem.class);
        when(frule1.getRepositoryId()).thenReturn("facetRule1");
        facetRules.add(frule1);

        Map<String, SolrDocument> ruleDocs = new HashMap<String, SolrDocument>();
        SolrDocument facetRuleDoc = mock(SolrDocument.class);
        when(facetRuleDoc.getFieldValue(FacetRuleProperty.COMBINE_MODE)).thenReturn(FacetRuleProperty.COMBINE_MODE_REPLACE);
        ruleDocs.put("facetRule1", facetRuleDoc);

        List<RepositoryItem> boostRules = new ArrayList<RepositoryItem>();
        typeToRules.put(boostRule, boostRules);
        RepositoryItem borule1 = mock(RepositoryItem.class);
        boostRules.add(borule1);
        

        List<RepositoryItem> blockRules = new ArrayList<RepositoryItem>();
        typeToRules.put(blockRule, blockRules);
        RepositoryItem blrule1 = mock(RepositoryItem.class);
        blockRules.add(blrule1);
                
        // this should NOT throw if no FACETS, BOOSTED_PRODUCTS or BLOCKED_PRODUCTS are found
        try {
            mgr.setRuleParams(query, typeToRules, ruleDocs);
        } catch (NullPointerException ex) {
            fail("Should protect against no rules set");
        }
    }

    @Test
    public void testSetRuleParamsWithSort() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        when(query.getSortFields()).thenReturn(new String[] {"reviewAverage desc", "reviews asc", "reviews desc", "score asc"});
        mgr.setRuleParams(query, new HashMap());
        verify(query).getSortFields();
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("reviewAverage", ORDER.desc);
        verify(query).addSortField("reviews", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
        verify(query).addSortField("_version_", ORDER.desc);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testRankingRuleForRuleBasedCategories() throws RepositoryException, SolrServerException {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrDocumentList ruleList = new SolrDocumentList();
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, ruleList);
        SolrDocument rule = new SolrDocument();
        rule.addField("description", "description facetRule2");
        rule.addField("id", "facetRule2");
        ruleList.add(rule);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1, false, ruleList);
        List<String> categories = new ArrayList<String>();
        categories.add("myCatalog.ruleBasedCategory");
        categories.add("__all__");
        setUpRuleData(categories, "description boostRule2", "boostRule2", boostRule, boostRuleItem2, false, ruleList);
        ruleList.setNumFound(ruleList.size()); 
        ruleList.setStart(0L);
        when(queryResponse.getResults()).thenReturn(ruleList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        mgr.loadRules("", "myCatalog.ruleBasedCategory", null, false, true, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        assertEquals(mgr.getRules().size(), 2);
        mgr.loadRules("", null, null, false, true, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        assertEquals(mgr.getRules().size(), 2);
    }
    
    @Test
    public void testSetRuleParamsWithSortAndBoostRule() {
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();
        List<RepositoryItem> rules = new ArrayList<RepositoryItem>();
        typeToRules.put(boostRule, rules);
        RepositoryItem rule = mock(RepositoryItem.class);
        rules.add(rule);
        List<RepositoryItem> boostedProducts = new ArrayList<RepositoryItem>();
        when(rule.getPropertyValue(BoostRuleProperty.BOOSTED_PRODUCTS)).thenReturn(boostedProducts);

        RepositoryItem boostedProduct1 = mock(RepositoryItem.class);
        boostedProducts.add(boostedProduct1);
        when(boostedProduct1.getRepositoryId()).thenReturn("boostedProduct1");

        RepositoryItem boostedProduct2 = mock(RepositoryItem.class);
        boostedProducts.add(boostedProduct2);
        when(boostedProduct2.getRepositoryId()).thenReturn("boostedProduct2");

        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        when(query.getSortFields()).thenReturn(new String[] {"reviewAverage desc", "reviews asc", "reviews desc", "score asc"});
        mgr.setRuleParams(query, typeToRules);
        verify(query, times(2)).getSortFields();
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("reviewAverage", ORDER.desc);
        verify(query).addSortField("reviews", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
        verify(query).addSortField("_version_", ORDER.desc);
        verifyNoMoreInteractions(query);
    }
    
    @Test
    public void testSetRuleParamsEmptyRules() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        mgr.setRuleParams(query, new HashMap());
        verify(query).getSortFields();
        verify(query).setSortField("isToos", ORDER.asc);
        verify(query).addSortField("score", ORDER.desc);
        verify(query).addSortField("_version_", ORDER.desc);
        verifyNoMoreInteractions(query);
    }    

    @Test
    public void testSetRuleParamsNullRules() {
        RuleManager mgr = new RuleManager(repository, builder, server);
        SolrQuery query = mock(SolrQuery.class);
        mgr.setRuleParams(query, null);
        verifyNoMoreInteractions(query);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoadRulesEmptyQuery() throws RepositoryException, SolrServerException {
        RuleManager mgr = new RuleManager(repository, builder, server);
        mgr.loadRules("", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
    }  
    
    @Test
    public void testLoadRulesNullRule() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        // ---------- set up docs with a rule type -----------
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        // note that we do NOT add this into the Repository so that we have a null rule in loadRules, this causes this document to not go into the rules
        SolrDocument rule = new SolrDocument();
        rule.addField("description", "description facetRule2");
        rule.addField("id", "facetRule2");
        solrDocumentList.add(rule);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1, false, solrDocumentList);
                       
        // ----------- set up doclist attributes ----------
        solrDocumentList.setNumFound(solrDocumentList.size());
        solrDocumentList.setStart(0L);
//        solrDocumentList.setMaxScore(1000.0);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ----------- set up rule manager -------------
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the rules that were generated ---------
        assertNotNull(mgr.getRules());
        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        
        assertEquals(2, rules.size());
        assertThat(rules.keySet(), hasItem(facetRule));
        assertThat(rules.keySet(), hasItem(boostRule));

        List<RepositoryItem> facetItems = rules.get(facetRule);
        List<RepositoryItem> boostItems = rules.get(boostRule);

        assertEquals(1, facetItems.size());
        assertEquals(1, boostItems.size());

        assertThat(facetItems, hasItem(facetRuleItem1));
        assertThat(boostItems, hasItem(boostRuleItem1));
    }        
    
    @Test
    public void testLoadRulesMixedTypes() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        // ---------- set up docs with a rule type -----------
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        setUpRuleData(null, "description blockRule1", "blockRule1", blockRule, blockRuleItem1, false, solrDocumentList);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1, false, solrDocumentList);
                       
        // ----------- set up doclist attributes ----------
        solrDocumentList.setNumFound(solrDocumentList.size()); 
        solrDocumentList.setStart(0L);
//        solrDocumentList.setMaxScore(1000.0);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ----------- set up rule manager -------------
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the rules that were generated ---------
        assertNotNull(mgr.getRules());
        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        
        assertEquals(3, rules.size());
        assertThat(rules.keySet(), hasItem(facetRule));
        assertThat(rules.keySet(), hasItem(boostRule));
        assertThat(rules.keySet(), hasItem(blockRule));

        List<RepositoryItem> facetItems = rules.get(facetRule);
        List<RepositoryItem> boostItems = rules.get(boostRule);
        List<RepositoryItem> blockItems = rules.get(blockRule);
        assertEquals(1, facetItems.size());
        assertEquals(1, boostItems.size());
        assertEquals(1, blockItems.size());
        assertThat(facetItems, hasItem(facetRuleItem1));
        assertThat(boostItems, hasItem(boostRuleItem1));
        assertThat(blockItems, hasItem(blockRuleItem1));
    }
    
    @Test
    public void testLoadRulesPaging() throws RepositoryException, SolrServerException {
        // test paging over batches of results
        SolrDocumentList docList1 = new SolrDocumentList();
        SolrDocumentList docList2 = new SolrDocumentList();
        
        RepositoryItem bikeItem = mock(RepositoryItem.class);
        RepositoryItem sledItem = mock(RepositoryItem.class);
        RepositoryItem carItem = mock(RepositoryItem.class);
        RepositoryItem heliItem = mock(RepositoryItem.class);
        RepositoryItem coatItem = mock(RepositoryItem.class);
        RepositoryItem snowItem = mock(RepositoryItem.class);
        RepositoryItem farmItem = mock(RepositoryItem.class);
        RepositoryItem steakItem = mock(RepositoryItem.class);
        RepositoryItem pillowItem = mock(RepositoryItem.class);

        // ---------- set up docs with a rule type -----------
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1,  false, docList1); // SKIPPING due to setStart
        setUpRuleData(null, "description blockRule1", "blockRule1", blockRule, blockRuleItem1, false, docList1);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1,false, docList1);
        setUpRuleData(null, "biking?  fun!", "tallboy", boostRule, bikeItem, false, docList1);
        setUpRuleData(null, "sleds are lame", "suzuki", facetRule, sledItem, false, docList1);
        setUpRuleData(null, "cars are lame", "vw", facetRule, carItem, false, docList1);
        setUpRuleData(null, "fly in a heli", "heli", boostRule, heliItem, false, docList2); // SKIPPING due to setStart
        setUpRuleData(null, "snow is fun", "snow", boostRule, snowItem, false, docList2);
        setUpRuleData(null, "good for food", "farm", blockRule, farmItem, false, docList2);
        setUpRuleData(null, "cows are food", "steak", facetRule, steakItem, false, docList2);
        setUpRuleData(null, "coatItem", "patagonia", boostRule, coatItem, false, docList2);
        setUpRuleData(null, "sleeping", "pillow", boostRule, pillowItem,false, docList2);
                       
        // ----------- set up doclist attributes ----------
        docList1.setNumFound(docList1.size() + docList2.size()); // set numfound to be both pagefuls...
        docList2.setNumFound(docList1.size() + docList2.size()); // set numfound to be both pagefuls...
        docList1.setStart(0L);
        docList2.setStart(0L);
//        solrDocumentList.setMaxScore(1000.0);
        QueryResponse queryResponse1 = mock(QueryResponse.class);
        QueryResponse queryResponse2 = mock(QueryResponse.class);
        
        when(queryResponse1.getResults()).thenReturn(docList1);
        when(queryResponse2.getResults()).thenReturn(docList2);
//        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse1, queryResponse2);
        
        // ----------- set up rule manager -------------        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the rules that were generated ---------
        assertNotNull(mgr.getRules());
        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        
        assertEquals(3, rules.size());
        assertThat(rules.keySet(), hasItem(facetRule));
        assertThat(rules.keySet(), hasItem(boostRule));
        assertThat(rules.keySet(), hasItem(blockRule));

        List<RepositoryItem> facetItems = rules.get(facetRule);
        List<RepositoryItem> boostItems = rules.get(boostRule);
        List<RepositoryItem> blockItems = rules.get(blockRule);
        
        assertEquals(4, facetItems.size());
        assertEquals(6, boostItems.size());
        assertEquals(2, blockItems.size());
        
        // test facets...
        for (RepositoryItem item : new RepositoryItem[]{sledItem, carItem, steakItem, }) {
            assertThat(facetItems, hasItem(item));    
        }
        
        // test boosts...
        for (RepositoryItem item : new RepositoryItem[]{boostRuleItem1, bikeItem, snowItem, coatItem, pillowItem,  }) {
            assertThat(boostItems, hasItem(item));    
        }
        
        // test blocks...
        for (RepositoryItem item : new RepositoryItem[]{ blockRuleItem1, farmItem, }) {
            assertThat(blockItems, hasItem(item));    
        }
    }
    
    @Test
    public void testLoadRulesFacets() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        // ---------- set up docs with a rule type -----------
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        setUpRuleData(null, "description facetRule2", "facetRule2", facetRule, blockRuleItem1, false, solrDocumentList);    
        setUpRuleData(null, "description facetRule3", "facetRule3", facetRule, boostRuleItem1, false, solrDocumentList);
                       
        // ----------- set up doclist attributes ----------
        solrDocumentList.setNumFound(solrDocumentList.size());
        solrDocumentList.setStart(0L);
//        solrDocumentList.setMaxScore(1000.0);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ----------- set up rule manager -------------
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the rules that were generated ---------
        assertNotNull(mgr.getRules());        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();        
        assertEquals(1, rules.size());
        assertEquals(facetRule, rules.keySet().iterator().next());        
        List<RepositoryItem> facetItems = rules.get(facetRule);        
        assertEquals(3, facetItems.size());
        assertThat(facetItems, hasItem(facetRuleItem1));
        assertThat(facetItems, hasItem(blockRuleItem1));
        assertThat(facetItems, hasItem(boostRuleItem1));
    }        
    
    // finished
    @Test
    public void testLoadRulesVerifyQueryWithCategory() throws RepositoryException, SolrServerException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "My super duper favorite Men's category";
        String searchQuery = "fantastic jackets )";
        String escapedSearchQuery = "fantastic\\ jackets\\ \\)";
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((" + escapedSearchQuery + ")^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    @Test
    public void testExcludeRule() throws RepositoryException, SolrServerException {
        
    }
    
    @Test
    public void testLoadRulesForCategoryPage() throws RepositoryException, SolrServerException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "My category";
        String searchQuery = "catId:myCat";
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, false, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("target:allpages OR target:categorypages", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    @Test
    public void testLoadRulesVerifyQueryWithoutCategory() throws RepositoryException, SolrServerException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "";
        String searchQuery = "fantastic jackets";
        String escapedSearchQuery = "fantastic\\ jackets";
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((fantastic\\ jackets)^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__", filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    // finished
    @Test
    public void testLoadRulesNullResults() throws RepositoryException, SolrServerException {
        // test the null queryResponse path
        when(queryResponse.getResults()).thenReturn(null);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ----------- set up rule manager -------------        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("pants", null, "Women's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        assertNotNull(mgr.getRules());
        assertEquals(0, mgr.getRules().size());
    }
    
    // finished
    @Test
    public void testLoadRulesEmptyResults() throws RepositoryException, SolrServerException {
        // test the empty queryResponse path
        SolrDocumentList solrDocumentList = new SolrDocumentList();        
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        // ----------- set up rule manager -------------        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules("pants", null, "Women's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        assertNotNull(mgr.getRules());
        assertEquals(0, mgr.getRules().size());
    }

    @Test
    public void testDefaultBoostFactors() {
        RuleManager mgr = new RuleManager(repository, builder, server);

        assertEquals(Float.toString(1/10f), mgr.mapStrength(RankingRuleProperty.STRENGTH_MAXIMUM_DEMOTE));
        assertEquals(Float.toString(1/5f), mgr.mapStrength(RankingRuleProperty.STRENGTH_STRONG_DEMOTE));
        assertEquals(Float.toString(1/2f), mgr.mapStrength(RankingRuleProperty.STRENGTH_MEDIUM_DEMOTE));
        assertEquals(Float.toString(1/1.5f), mgr.mapStrength(RankingRuleProperty.STRENGTH_WEAK_DEMOTE));
        assertEquals(Float.toString(1.0f), mgr.mapStrength(RankingRuleProperty.STRENGTH_NEUTRAL));
        assertEquals(Float.toString(1.5f), mgr.mapStrength(RankingRuleProperty.STRENGTH_WEAK_BOOST));
        assertEquals(Float.toString(2f), mgr.mapStrength(RankingRuleProperty.STRENGTH_MEDIUM_BOOST));
        assertEquals(Float.toString(5f), mgr.mapStrength(RankingRuleProperty.STRENGTH_STRONG_BOOST));
        assertEquals(Float.toString(10f), mgr.mapStrength(RankingRuleProperty.STRENGTH_MAXIMUM_BOOST));

    }

    @Test
    public void testFacetRuleSorting() {
        final FacetManager facetManager = mock(FacetManager.class);
        RuleManager mgr = new RuleManager(repository, builder, server) {
            @Override
            public FacetManager getFacetManager() {
                return facetManager;
            }
        };
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();

        List<RepositoryItem> facetRules = new ArrayList<RepositoryItem>();
        typeToRules.put(facetRule, facetRules);
        RepositoryItem facetRule1 = mock(RepositoryItem.class);
        when(facetRule1.getRepositoryId()).thenReturn("facetRule1");
        facetRules.add(facetRule1);

        RepositoryItem facet1 = mock(RepositoryItem.class, "facet1");
        RepositoryItem facet2 = mock(RepositoryItem.class, "facet2");
        RepositoryItem facet3 = mock(RepositoryItem.class, "facet3");

        when(facetRule1.getPropertyValue(FacetRuleProperty.FACETS)).thenReturn(Arrays.asList(facet1, facet2, facet3));

        Map<String, SolrDocument> ruleDocs = new HashMap<String, SolrDocument>();
        SolrDocument facetRuleDoc = mock(SolrDocument.class);
        when(facetRuleDoc.getFieldValue(FacetRuleProperty.COMBINE_MODE)).thenReturn(FacetRuleProperty.COMBINE_MODE_REPLACE);
        ruleDocs.put("facetRule1", facetRuleDoc);


        mgr.setRuleParams(query, typeToRules, ruleDocs);
        verify(facetManager).clear();
        verify(facetManager).addFacet(facet1);
        verify(facetManager).addFacet(facet2);
        verify(facetManager).addFacet(facet3);
        verify(facetManager).setParams(query);
        verifyNoMoreInteractions(facetManager);
    }

    @Test
    public void testFacetRuleSortingReplace() {
        testFacetRuleSortingAux(FacetRuleProperty.COMBINE_MODE_REPLACE, 2);
    }

    @Test
    public void testFacetRuleSortingAppend() {
        testFacetRuleSortingAux(FacetRuleProperty.COMBINE_MODE_APPEND, 0);
    }

    @Test
    public void testOutletCategoryRule () throws SolrServerException, RepositoryException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "My category";
        String searchQuery = "catId:myCat";
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, false, false, cataA, true, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("target:allpages OR target:categorypages", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Outlet", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    @Test
    public void testRetailCategoryRule() throws SolrServerException, RepositoryException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "My category";
        String searchQuery = "catId:myCat";
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, false, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("target:allpages OR target:categorypages", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    @Test
    public void testBrandCategoryRule() throws SolrServerException, RepositoryException {
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        String category = "My category";
        String searchQuery = "catId:myCat";
        String brandId = "54"; 
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        // ----------- set up rule manager -------------
        RuleManager mgr = new RuleManager(repository, builder, server);
        
        // ------------ make the call to load the rules etc -------------
        mgr.loadRules(searchQuery, null, category, false, false, cataA, false, brandId, new HashSet<String>(), new HashSet<String>());
        
        // ------------ assertions about the inner solr query that was performed -----------
        ArgumentCaptor<SolrQuery> query = ArgumentCaptor.forClass(SolrQuery.class);
        verify(server).query(query.capture());
        List<String> filters = Arrays.asList(query.getValue().getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", query.getValue().getQuery());
        assertEquals("target:allpages OR target:categorypages", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__ OR brandId:54", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }
    
    private void testFacetRuleSortingAux(String combineMode, int clearTimes) {
        final FacetManager facetManager = mock(FacetManager.class);
        RuleManager mgr = new RuleManager(repository, builder, server) {
            @Override
            public FacetManager getFacetManager() {
                return facetManager;
            }
        };
        SolrQuery query = mock(SolrQuery.class);
        Map<String, List<RepositoryItem>> typeToRules = new HashMap<String, List<RepositoryItem>>();

        List<RepositoryItem> facetRules = new ArrayList<RepositoryItem>();
        Map<String, SolrDocument> ruleDocs = new HashMap<String, SolrDocument>();


        // first rule
        RepositoryItem facetRule1 = mock(RepositoryItem.class, "facetRule1");
        when(facetRule1.getRepositoryId()).thenReturn("facetRule1");
        facetRules.add(facetRule1);

        SolrDocument facetRuleDoc = mock(SolrDocument.class);
        when(facetRuleDoc.getFieldValue(FacetRuleProperty.COMBINE_MODE)).thenReturn(combineMode);
        ruleDocs.put("facetRule1", facetRuleDoc);

        RepositoryItem facet1 = mock(RepositoryItem.class, "facet1");
        RepositoryItem facet2 = mock(RepositoryItem.class, "facet2");
        RepositoryItem facet3 = mock(RepositoryItem.class, "facet3");

        when(facetRule1.getPropertyValue(FacetRuleProperty.FACETS)).thenReturn(Arrays.asList(facet1, facet2, facet3));
        typeToRules.put(facetRule, facetRules);

        // second rule
        RepositoryItem facetRule2 = mock(RepositoryItem.class, "facetRule2");
        when(facetRule2.getRepositoryId()).thenReturn("facetRule2");
        facetRules.add(facetRule2);

        SolrDocument facetRuleDoc2 = mock(SolrDocument.class);
        when(facetRuleDoc2.getFieldValue(FacetRuleProperty.COMBINE_MODE)).thenReturn(combineMode);
        ruleDocs.put("facetRule2", facetRuleDoc2);

        RepositoryItem facet4 = mock(RepositoryItem.class, "facet4");
        RepositoryItem facet5 = mock(RepositoryItem.class, "facet5");
        RepositoryItem facet6 = mock(RepositoryItem.class, "facet6");

        when(facetRule2.getPropertyValue(FacetRuleProperty.FACETS)).thenReturn(Arrays.asList(facet4, facet5, facet6));
        typeToRules.put(facetRule, facetRules);

        mgr.setRuleParams(query, typeToRules, ruleDocs);
        verify(facetManager, times(clearTimes)).clear();
        verify(facetManager).addFacet(facet1);
        verify(facetManager).addFacet(facet2);
        verify(facetManager).addFacet(facet3);
        verify(facetManager).addFacet(facet4);
        verify(facetManager).addFacet(facet5);
        verify(facetManager).addFacet(facet6);
        verify(facetManager).setParams(query);
        verifyNoMoreInteractions(facetManager);
    }
    
    @Test
    public void testIncludeRulesExperimental() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, true, solrDocumentList);
        setUpRuleData(null, "description blockRule1", "blockRule1", blockRule, blockRuleItem1, true, solrDocumentList);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1, true, solrDocumentList);                       
        solrDocumentList.setNumFound(solrDocumentList.size());
        solrDocumentList.setStart(0L);
        
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        
        Set<String> includeRules = new HashSet<String>();
        includeRules.add("facetRule1");
        includeRules.add("blockRule1");
        
        Set<String> excludeRules = new HashSet<String>();
        excludeRules.add("boostRule1");
        
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, includeRules, excludeRules);
        
        assertNotNull(mgr.getRules());        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        assertEquals(2, rules.size());
        List<RepositoryItem> blockItems = rules.get(blockRule);        
        assertEquals(1, blockItems.size());
        assertThat(blockItems, hasItem(blockRuleItem1));
        
        List<RepositoryItem> facetItems = rules.get(facetRule);        
        assertEquals(1, facetItems.size());
        assertThat(facetItems, hasItem(facetRuleItem1));

    }        
    
    @Test
    public void testIncludeRulesNotExperimental() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, false, solrDocumentList);
        setUpRuleData(null, "description facetRule2", "facetRule2", facetRule, facetRuleItem2, false, solrDocumentList);    
        setUpRuleData(null, "description facetRule3", "facetRule3", facetRule, facetRuleItem3, true, solrDocumentList);                       
        solrDocumentList.setNumFound(solrDocumentList.size());
        solrDocumentList.setStart(0L);
        
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
                
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), new HashSet<String>());
        
        assertNotNull(mgr.getRules());        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        assertEquals(1, rules.size());
        assertEquals(facetRule, rules.keySet().iterator().next());        
        List<RepositoryItem> facetItems = rules.get(facetRule);        
        assertEquals(2, facetItems.size());
        assertThat(facetItems, hasItem(facetRuleItem1));
        assertThat(facetItems, hasItem(facetRuleItem2));  
    }        
    
    @Test
    public void testExcludeRules() throws RepositoryException, SolrServerException {  
        SolrDocumentList solrDocumentList = new SolrDocumentList();
        setUpRuleData(null, "description facetRule1", "facetRule1", facetRule, facetRuleItem1, true, solrDocumentList);
        setUpRuleData(null, "description blockRule1", "blockRule1", blockRule, blockRuleItem1, true, solrDocumentList);    
        setUpRuleData(null, "description boostRule1", "boostRule1", boostRule, boostRuleItem1, true, solrDocumentList);                       
        solrDocumentList.setNumFound(solrDocumentList.size());
        solrDocumentList.setStart(0L);
        
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(server.query(any(SolrParams.class))).thenReturn(queryResponse);
        
        RuleManager mgr = new RuleManager(repository, builder, server);
        assertEquals(null, mgr.getRules());
        HashSet<String> excludeRules = new HashSet<String>();
        excludeRules.add("facetRule1");
        excludeRules.add("facetRule2");
        excludeRules.add("facetRule3");
        mgr.loadRules("jackets", null, "Men's Clothing", true, false, cataA, false, null, new HashSet<String>(), excludeRules);
        
        assertNotNull(mgr.getRules());        
        Map<String, List<RepositoryItem>> rules = mgr.getRules();
        assertEquals(0, rules.size());
      
    }        
}
