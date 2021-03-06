package org.apache.solr.handler.component;

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

import com.google.common.collect.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencommercesearch.FacetConstants;
import org.opencommercesearch.RuleConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.hamcrest.CoreMatchers.hasItems;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Javier Mendez
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SolrCore.class})
@PowerMockIgnore("org.jacoco.agent.rt.*")
public class RuleManagerComponentTest {

    enum TestSetType { empty, blockRules, boostRules, facetRules, facetRulesReplace, facetRulesAppend, rulesButNoContent, rankingRule }

    FieldType defaultFieldType = new FieldType();

    {
        defaultFieldType.setStored(true);
    }

    @Mock private ResponseBuilder rb;
    @Mock private SolrQueryRequest req;
    @Mock private SolrQueryResponse rsp;
    @Mock private CoreContainer coreContainer;
    private SolrCore rulesCore;
    @Mock private SearchHandler searchHandler;
    @Mock private SolrIndexSearcher rulesIndexSearcher;

    private SolrCore facetsCore;
    @Mock private SolrIndexSearcher facetsIndexSearcher;

    RuleManagerComponent component = new RuleManagerComponent();
    ModifiableSolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        rulesCore = PowerMockito.mock(SolrCore.class);
        facetsCore = PowerMockito.mock(SolrCore.class);

        rb.req = req;
        rb.rsp = rsp;
        component.rulesCoreName = "rulesCore";
        component.facetsCoreName = "facetsCore";
        component.coreContainer = coreContainer;
        component.initSeasonMappings(null);

        when(req.getParams()).thenReturn(params);
        when(coreContainer.getCore("rulesCore")).thenReturn(rulesCore);
        when(coreContainer.getCore("facetsCore")).thenReturn(facetsCore);
        when(rulesCore.getRequestHandler("/select")).thenReturn(searchHandler);
        when(facetsCore.getRequestHandler("/select")).thenReturn(searchHandler);

        when(rulesCore.getSearcher()).thenReturn(new RefCounted<SolrIndexSearcher>(rulesIndexSearcher) {
            @Override
            protected void close() {
            }
        });

        when(facetsCore.getSearcher()).thenReturn(new RefCounted<SolrIndexSearcher>(facetsIndexSearcher) {
            @Override
            protected void close() {}
        });
    }

    @Test
    public void testInitComponent() throws IOException {
        NamedList<String> list = new NamedList<String>();
        list.add("seasonMapping", "expA:1,2,3,4,5,6;expB:7,8,9,10,11,12");
        component.init(list);
        int i = 1;
        for(String actualSeason: component.seasonMapper) {
            if (i <= 6) {
                assertEquals("expA", actualSeason);
            } else {
                assertEquals("expB", actualSeason);
            }
            i++;
        }
    }

    @Test
    public void testInitComponentIncorrectSeasonMapping() throws IOException {
        NamedList<String> list = new NamedList<String>();
        list.add("seasonMapping", "someIncorrectMapping");
        component.init(list);
        int i = 1;
        for(String actualSeason: component.seasonMapper) {
            assertEquals(RuleManagerComponent.DEFAULT_SEASON_MAPPER, actualSeason);
            i++;
        }
    }

    @Test
    public void testInitComponentWithSomeDefaults() throws IOException {
        NamedList<String> list = new NamedList<String>();
        list.add("seasonMapping", "expA:1,2,3,4,5,6");
        component.init(list);
        int i = 1;
        for(String actualSeason: component.seasonMapper) {
            if (i <= 6) {
                assertEquals("expA", actualSeason);
            } else {
                assertEquals(RuleManagerComponent.DEFAULT_SEASON_MAPPER, actualSeason);
            }
            i++;
        }
    }

    @Test
    public void testNoPageType() throws IOException {
        //Should do nothing (no exceptions)
        component.prepare(rb);
        verify(req, never()).setParams(any(SolrParams.class));
    }

    @Test
    public void testNoRules() throws IOException {
        prepareRuleDocs(TestSetType.empty);
        setBaseParams();

        //Should set basic params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
        assertEquals("category:0.paulcatalog", outParams.get(CommonParams.FQ));
    }

    @Test
    public void testBlockRules() throws IOException {
        prepareRuleDocs(TestSetType.blockRules);
        setBaseParams();
        //Should filter out specified products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
        String[] filterQueries = outParams.getParams(CommonParams.FQ);
        assertEquals(3, filterQueries.length);
        assertEquals("-productId:product0", filterQueries[0]);
        assertEquals("-productId:product1", filterQueries[1]);
        assertEquals("category:0.paulcatalog", filterQueries[2]);
    }

    @Test
    public void testBoostRules() throws IOException {
        prepareRuleDocs(TestSetType.boostRules);
        setBaseParams();

        //Should set boost for given products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,fixedBoost(productId,'product2') asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }
    
    @Test
    public void testExcludeBoostRulesExperiment() throws IOException {
        prepareRuleDocs(TestSetType.boostRules);
        setBaseParams();
        params.set("excludeRules", "0,1");
        //Should set boost for given products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }

    @Test
    public void testIncludeBoostRulesExperiment() throws IOException {
        prepareRuleDocs(TestSetType.boostRules);
        setBaseParams();
        params.set("includeRules", "1");
        //Should set boost for given products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,fixedBoost(productId,'product3') asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }


    @Test
    public void testBoostRulesRuleCatPage() throws IOException {
        prepareRuleDocs(TestSetType.boostRules);
        setBaseParams();
        params.set(RuleManagerParams.RULE_PAGE, "true");
        params.set(RuleManagerParams.CATEGORY_FILTER, ClientUtils.escapeQueryChars("rule cat page"));
        
        //Should set boost for given products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,fixedBoost(productId,'product3') asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }

    @Test
    public void testRankingRules() throws IOException {
        prepareRuleDocs(TestSetType.rankingRule);
        setBaseParams();

        //Should set ranking rules for given products
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();

        Calendar calendar = Calendar.getInstance();
        String expectedSeason = component.seasonMapper[calendar.get(Calendar.MONTH)];
        String expectedYear = Integer.toString(calendar.get(Calendar.YEAR));
        String expectedBoost = "if(exists(query({!lucene v='(season:" + expectedSeason + " OR year:" + expectedYear + " OR season:SS1)'})),0.5,1.0)";
        assertEquals(expectedBoost, outParams.get("boost"));
    }

    @Test
    public void testFacetRules() throws IOException {
        //TODO: make a good test for testFacetRules
        prepareRuleDocs(TestSetType.facetRules);
        setBaseParams();

        //Should set basic facet params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));

        String[] facetFields = outParams.getParams(FacetParams.FACET_FIELD);
        assertEquals(2, facetFields.length);
        assertEquals("{!ex=collapse}fieldName1", facetFields[0]);
        assertEquals("{!ex=collapse}fieldName2", facetFields[1]);
        assertEquals("true", outParams.get(FacetParams.FACET));
    }

    @Test
    public void testLocaleFacetRules() throws IOException {
        testLocaleFacetRules(null, null);
    }

    @Test
    public void testLocaleFacetRulesWithCountry() throws IOException {
        testLocaleFacetRules("US", null);
    }

    @Test
    public void testLocaleFacetRulesWithNoDefaultCountry() throws IOException {
        testLocaleFacetRules("CR", null);
    }

    @Test
    public void testLocaleFacetRulesWithCountryAndSite() throws IOException {
        testLocaleFacetRules("US", "paulcatalog");
    }

    @Test
    public void testLocaleFacetRulesWithSite() throws IOException {
        testLocaleFacetRules(null, "paulcatalog");
    }


    private void testLocaleFacetRules(String country, String site) throws IOException {
        prepareRuleFacetDocs(country, site, true);
        setBaseParams(country);

        //Should set basic facet params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));

        String[] facetFields = outParams.getParams(FacetParams.FACET_FIELD);
        assertEquals(2, facetFields.length);
        assertEquals("{!ex=collapse}" + fieldName("fieldName1", country, site), facetFields[0]);
        assertEquals("{!ex=collapse}" + fieldName("fieldName2", country, site), facetFields[1]);
        String[] rangeFields = outParams.getParams(FacetParams.FACET_RANGE);
        assertEquals(1, rangeFields.length);
        String fieldName3 = fieldName("fieldName3", country, site);
        assertEquals("{!ex=collapse}"  + fieldName3, rangeFields[0]);
        assertEquals(Integer.valueOf(0), outParams.getFieldInt(fieldName3, FacetParams.FACET_RANGE_START));

        String[] queryFields = outParams.getParams(FacetParams.FACET_QUERY);
        String fieldName4 = fieldName("fieldName4", country, site);
        assertEquals("{!ex=collapse}"  + fieldName4 + ":fq", queryFields[0]);
        assertEquals("true", outParams.get(FacetParams.FACET));
    }

    private String fieldName(String name, String country, String site) {
        if (country == null && site == null) {
            return name;
        }
        if (country != null && !name.endsWith(country)) {
            name += country;
        }
        if (site != null) {
            name += site;
        }
        return name;
    }

    @Test
    public void testFacetRulesReplace() throws IOException {
        //TODO: make a good test for testFacetRules
        prepareRuleDocs(TestSetType.facetRulesReplace);
        setBaseParams();

        //Should set basic facet params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
        assertEquals("{!ex=collapse}fieldName3", outParams.get(FacetParams.FACET_FIELD));
        assertEquals("true", outParams.get(FacetParams.FACET));
    }

    @Test
    public void testFacetRulesAppend() throws IOException {
        //TODO: make a good test for testFacetRules
        prepareRuleDocs(TestSetType.facetRulesAppend);
        setBaseParams();

        //Should set basic facet params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));

        String[] facetFields = outParams.getParams(FacetParams.FACET_FIELD);
        assertEquals(3, facetFields.length);
        assertEquals("{!ex=collapse}fieldName1", facetFields[0]);
        assertEquals("{!ex=collapse}fieldName2", facetFields[1]);
        assertEquals("{!ex=collapse}fieldName3", facetFields[2]);
        assertEquals("true", outParams.get(FacetParams.FACET));
    }

    @Test
    public void testRulesNoContent() throws IOException {
        prepareRuleDocs(TestSetType.rulesButNoContent);
        setBaseParams();

        //Should re-order sort values and ignore  duplicates
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }

    @Test
    public void testQueryWithSorts() throws IOException {
        prepareRuleDocs(TestSetType.rulesButNoContent);
        setBaseParams();
        params.set(CommonParams.SORT, "reviewAverage desc,reviews asc,reviews desc,score asc");

        //Should do nothing (including not failing!)
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,reviewAverage desc,reviews asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }

    @Test
    public void testBoostRulesWithSorts() throws IOException {
        prepareRuleDocs(TestSetType.boostRules);
        setBaseParams();
        params.set(CommonParams.SORT, "reviewAverage desc,reviews asc,reviews desc,score asc");

        //Should ignore boost for given products and prefer the incoming sort options
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,reviewAverage desc,reviews asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));
    }

    @Test
    public void testRulesCategoryPage() throws IOException {
        prepareRuleDocs(TestSetType.empty);
        setBaseParams();
        params.set(RuleManagerParams.PAGE_TYPE, "category");
        params.set(RuleManagerParams.RULE_PAGE, "true");
        params.add(CommonParams.FQ, "(someField:val OR season:$SEASON OR year:$YEAR)");
        params.add(CommonParams.FQ, "otherFilter:val");

        NamedList<String> list = new NamedList<String>();
        list.add("seasonMapping", "expA:1,2,3,4,5,6;expB:7,8,9,10,11,12");
        component.init(list);

        //Should set basic params
        component.prepare(rb);
        ArgumentCaptor<MergedSolrParams> argumentCaptor = ArgumentCaptor.forClass(MergedSolrParams.class);
        verify(req).setParams(argumentCaptor.capture());

        SolrParams outParams = argumentCaptor.getValue();
        assertEquals("isToos asc,score desc,_version_ desc", outParams.get(CommonParams.SORT));
        assertEquals("1.paulcatalog.", outParams.get("f.category.facet.prefix"));

        Calendar calendar = Calendar.getInstance();
        String expectedSeason = component.seasonMapper[calendar.get(Calendar.MONTH)];
        String expectedYear = Integer.toString(calendar.get(Calendar.YEAR));

        List<String> fqList = Lists.newArrayList(outParams.getParams(CommonParams.FQ));
        assertThat(fqList, hasItems("category:0.paulcatalog",
                                    "otherFilter:val",
                                    "(someField:val OR season:" + expectedSeason + " OR year:" + expectedYear + ")"));
    }

    private void setBaseParams() {
        setBaseParams(null);
    }

    private void setBaseParams(String country) {
        params.set(RuleManagerParams.RULE, "true");
        params.set(RuleManagerParams.PAGE_TYPE, "search");
        params.set(RuleManagerParams.CATALOG_ID, "paulcatalog");
        params.set(CommonParams.Q, "some books");
        params.set(FacetParams.FACET, true);
        if (country != null) {
            params.set(RuleManagerParams.COUNTRY_ID, country);
        }
    }

    /**
     * Helper method that associates test result sets based on the current test.
     */
    private void prepareRuleDocs(TestSetType setType) throws IOException {
        switch (setType) {
            case empty: {
                setIdsToResultContext(new int[]{}, rulesCore);
                break;
            }

            case rankingRule: {
                setIdsToResultContext(new int[]{0}, rulesCore);
                Document rankRule = new Document();
                rankRule.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
                rankRule.add(new Field(RuleConstants.FIELD_BOOST_FUNCTION, "if(exists(query({!lucene v='(season:$SEASON OR year:$YEAR OR season:SS1)'})),0.5,1.0)", defaultFieldType));
                rankRule.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.rankingRule.toString(), defaultFieldType));
                when(rulesIndexSearcher.doc(0)).thenReturn(rankRule);
                break;
            }

            case blockRules: {
                setIdsToResultContext(new int[]{0, 1}, rulesCore);
                Document blockRule1 = new Document();
                blockRule1.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
                blockRule1.add(new Field(RuleConstants.FIELD_BLOCKED_PRODUCTS, "product0", defaultFieldType));
                blockRule1.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.blockRule.toString(), defaultFieldType));

                Document blockRule2 = new Document();
                blockRule2.add(new Field(RuleConstants.FIELD_ID, "1", defaultFieldType));
                blockRule2.add(new Field(RuleConstants.FIELD_BLOCKED_PRODUCTS, "product1", defaultFieldType));
                blockRule2.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.blockRule.toString(), defaultFieldType));
                when(rulesIndexSearcher.doc(1)).thenReturn(blockRule2);
                when(rulesIndexSearcher.doc(0)).thenReturn(blockRule1);
                break;
            }

            case boostRules: {
                setIdsToResultContext(new int[]{0, 1}, rulesCore);
                Document boostRule1 = new Document();
                boostRule1.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
                boostRule1.add(new Field(RuleConstants.FIELD_CATEGORY, "_all_", defaultFieldType));
                boostRule1.add(new Field(RuleConstants.FIELD_BOOSTED_PRODUCTS, "product2", defaultFieldType));
                boostRule1.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.boostRule.toString(), defaultFieldType));
                boostRule1.add(new Field(RuleConstants.FIELD_EXPERIMENTAL, "true", defaultFieldType));

                Document boostRule2 = new Document();
                boostRule2.add(new Field(RuleConstants.FIELD_ID, "1", defaultFieldType));
                boostRule2.add(new Field(RuleConstants.FIELD_CATEGORY, "_all_", defaultFieldType));
                boostRule2.add(new Field(RuleConstants.FIELD_CATEGORY, "rule cat page", defaultFieldType));
                boostRule2.add(new Field(RuleConstants.FIELD_BOOSTED_PRODUCTS, "product3", defaultFieldType));
                boostRule2.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.boostRule.toString(), defaultFieldType));
                boostRule2.add(new Field(RuleConstants.FIELD_EXPERIMENTAL, "true", defaultFieldType));
                
                when(rulesIndexSearcher.doc(0)).thenReturn(boostRule1);
                when(rulesIndexSearcher.doc(1)).thenReturn(boostRule2);
                break;
            }

            case facetRules: {
                prepareRuleFacetDocs(null, null, true);
                break;
            }

            case facetRulesReplace:
            case facetRulesAppend: {

                String combineMode = RuleConstants.COMBINE_MODE_REPLACE;
                if(setType == TestSetType.facetRulesAppend) {
                    combineMode = RuleConstants.COMBINE_MODE_APPEND;
                }

                setIdsToResultContext(new int[]{0, 1}, rulesCore);
                Document facetRule1 = new Document();
                facetRule1.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
                facetRule1.add(new Field(RuleConstants.FIELD_FACET_ID, "facet1", defaultFieldType));
                facetRule1.add(new Field(RuleConstants.FIELD_FACET_ID, "facet2", defaultFieldType));
                facetRule1.add(new Field(RuleConstants.FIELD_COMBINE_MODE, combineMode, defaultFieldType));
                facetRule1.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.facetRule.toString(), defaultFieldType));

                Document facetRule2 = new Document();
                facetRule2.add(new Field(RuleConstants.FIELD_ID, "1", defaultFieldType));
                facetRule2.add(new Field(RuleConstants.FIELD_FACET_ID, "facet3", defaultFieldType));
                facetRule2.add(new Field(RuleConstants.FIELD_COMBINE_MODE, combineMode, defaultFieldType));
                facetRule2.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.facetRule.toString(), defaultFieldType));

                when(rulesIndexSearcher.doc(0)).thenReturn(facetRule1);
                when(rulesIndexSearcher.doc(1)).thenReturn(facetRule2);

                setIdsToResultContext(new int[]{0, 1}, new int[]{2}, facetsCore);

                Document facet1 = new Document();
                facet1.add(new Field(FacetConstants.FIELD_ID, "facet1", defaultFieldType));
                facet1.add(new Field(FacetConstants.FIELD_FIELD_NAME, "fieldName1", defaultFieldType));
                facet1.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_FIELD, defaultFieldType));

                Document facet2= new Document();
                facet2.add(new Field(FacetConstants.FIELD_ID, "facet2", defaultFieldType));
                facet2.add(new Field(FacetConstants.FIELD_FIELD_NAME, "fieldName2", defaultFieldType));
                facet2.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_FIELD, defaultFieldType));

                Document facet3= new Document();
                facet3.add(new Field(FacetConstants.FIELD_ID, "facet3", defaultFieldType));
                facet3.add(new Field(FacetConstants.FIELD_FIELD_NAME, "fieldName3", defaultFieldType));
                facet3.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_FIELD, defaultFieldType));

                when(facetsIndexSearcher.doc(0)).thenReturn(facet1);
                when(facetsIndexSearcher.doc(1)).thenReturn(facet2);
                when(facetsIndexSearcher.doc(2)).thenReturn(facet3);
                break;
            }

            case rulesButNoContent: {
                setIdsToResultContext(new int[]{0, 1, 2}, rulesCore);
                Document facetRule = new Document();
                facetRule.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
                facetRule.add(new Field(RuleConstants.FIELD_COMBINE_MODE, RuleConstants.COMBINE_MODE_REPLACE, defaultFieldType));
                facetRule.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.facetRule.toString(), defaultFieldType));

                Document boostRule = new Document();
                boostRule.add(new Field(RuleConstants.FIELD_ID, "1", defaultFieldType));
                boostRule.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.boostRule.toString(), defaultFieldType));

                Document blockRule = new Document();
                blockRule.add(new Field(RuleConstants.FIELD_ID, "2", defaultFieldType));
                blockRule.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.blockRule.toString(), defaultFieldType));

                when(rulesIndexSearcher.doc(0)).thenReturn(facetRule);
                when(rulesIndexSearcher.doc(1)).thenReturn(boostRule);
                when(rulesIndexSearcher.doc(2)).thenReturn(blockRule);

                break;
            }
        }
    }

    private void prepareRuleFacetDocs(String country, String site, boolean allTypes) throws IOException {
        setIdsToResultContext(new int[]{0}, rulesCore);
        Document facetRule = new Document();
        facetRule.add(new Field(RuleConstants.FIELD_ID, "0", defaultFieldType));
        facetRule.add(new Field(RuleConstants.FIELD_FACET_ID, "facet1", defaultFieldType));
        facetRule.add(new Field(RuleConstants.FIELD_FACET_ID, "facet2", defaultFieldType));
        facetRule.add(new Field(RuleConstants.FIELD_COMBINE_MODE, RuleConstants.COMBINE_MODE_REPLACE, defaultFieldType));
        facetRule.add(new Field(RuleConstants.FIELD_RULE_TYPE, RuleManagerComponent.RuleType.facetRule.toString(), defaultFieldType));

        when(rulesIndexSearcher.doc(0)).thenReturn(facetRule);

        if (allTypes) {
            setIdsToResultContext(new int[]{0, 1, 2, 3}, facetsCore);
        } else {
            setIdsToResultContext(new int[]{0, 1}, facetsCore);
        }

        Document facet1 = new Document();
        String field1 = "fieldName1";
        facet1.add(new Field(FacetConstants.FIELD_ID, "facet1", defaultFieldType));
        facet1.add(new Field(FacetConstants.FIELD_FIELD_NAME, field1, defaultFieldType));
        facet1.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_FIELD, defaultFieldType));
        setCountryAndSiteFields(country, site, facet1);

        Document facet2= new Document();
        String field2 = "fieldName2";
        if (country != null) {
            // test backwards compatibility
            field2 += country;
        }
        facet2.add(new Field(FacetConstants.FIELD_ID, "facet2", defaultFieldType));
        facet2.add(new Field(FacetConstants.FIELD_FIELD_NAME, field2, defaultFieldType));
        facet2.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_FIELD, defaultFieldType));
        setCountryAndSiteFields(country, site, facet2);

        when(facetsIndexSearcher.doc(0)).thenReturn(facet1);
        when(facetsIndexSearcher.doc(1)).thenReturn(facet2);

        if (allTypes) {
            Document rangeFacet = new Document();
            String field3 = "fieldName3";
            rangeFacet.add(new Field(FacetConstants.FIELD_ID, "facet3", defaultFieldType));
            rangeFacet.add(new Field(FacetConstants.FIELD_FIELD_NAME, field3, defaultFieldType));
            rangeFacet.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_RANGE, defaultFieldType));
            setCountryAndSiteFields(country, site, rangeFacet);
            when(facetsIndexSearcher.doc(2)).thenReturn(rangeFacet);


            Document queryFacet = new Document();
            String field4 = "fieldName4";
            queryFacet.add(new Field(FacetConstants.FIELD_ID, "facet3", defaultFieldType));
            queryFacet.add(new Field(FacetConstants.FIELD_FIELD_NAME, field4, defaultFieldType));
            queryFacet.add(new Field(FacetConstants.FIELD_TYPE, FacetConstants.FACET_TYPE_QUERY, defaultFieldType));
            queryFacet.add(new Field(FacetConstants.FIELD_QUERIES, "fq", defaultFieldType));
            setCountryAndSiteFields(country, site, queryFacet);
            when(facetsIndexSearcher.doc(3)).thenReturn(queryFacet);
        }
    }

    private void setCountryAndSiteFields(String country, String site, Document facet) {
        if (country != null) {
            facet.add(new Field(FacetConstants.FIELD_BY_COUNTRY, "true", defaultFieldType));
        }
        if (site != null) {
            facet.add(new Field(FacetConstants.FIELD_BY_SITE, "true", defaultFieldType));
        }
    }

    /**
     * Sets IDs to a result context and associates it with rulesCore.execute method.
     * @param ids Ids to set.
     */
    private void setIdsToResultContext(int[] ids, SolrCore core) {
        setIdsToResultContext(ids, null, core);
    }

    /**
     * Sets IDs to a result context and associates it with rulesCore.execute method.
     * @param ids1 Ids to set.
     * @param ids2 Second set of ids to return. If null, only one set is returned.
     */
    private void setIdsToResultContext(int[] ids1, int[]ids2, SolrCore core) {
        final ResultContext result = new ResultContext();
        result.docs = new DocSlice(0, ids1.length, ids1, null, 0, ids1.length);

        if(ids2 == null) {
            doAnswer(new ResponseSetterAnswer(result)).when(core).execute((SolrRequestHandler) anyObject(), (SolrQueryRequest) anyObject(), (SolrQueryResponse) anyObject());
        }
        else {
            final ResultContext secondResult = new ResultContext();
            secondResult.docs = new DocSlice(0, ids2.length, ids2, null, 0, ids2.length);
            doAnswer(new ResponseSetterAnswer(result, secondResult)).when(core).execute((SolrRequestHandler) anyObject(), (SolrQueryRequest) anyObject(), (SolrQueryResponse) anyObject());
        }
    }

    /**
     * Helper answer class to set the response context on SolrCore.execute calls.
     */
    class ResponseSetterAnswer implements Answer<Object> {
        ResultContext result;
        ResultContext secondResult;

        int callCounter = 0;

        public ResponseSetterAnswer(ResultContext result) {
            this.result = result;
        }

        public ResponseSetterAnswer(ResultContext result, ResultContext secondResult) {
            this.result = result;
            this.secondResult = secondResult;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            SolrQueryResponse response = (SolrQueryResponse) invocationOnMock.getArguments()[2];

            if(callCounter == 0 || secondResult == null) {
                response.add("response", result);
            }
            else {
                response.add("response", secondResult);
            }

            callCounter++;
            return null;
        }
    }

    @Test
    public void testLoadRulesVerifyQueryWithCategory() throws IOException {
        String category = "My super duper favorite Men's category";
        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.add(CommonParams.Q, "fantastic jackets");
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(RuleManagerParams.CATEGORY_FILTER, category);
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        RuleManagerComponent mgr = new RuleManagerComponent();
        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((fantastic jackets)^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesVerifyQueryWithWildCardStartColonStar() throws IOException {
        String category = "My super duper favorite Men's category";
        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.add(CommonParams.Q, "*:*");
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(RuleManagerParams.CATEGORY_FILTER, category);
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        RuleManagerComponent mgr = new RuleManagerComponent();
        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND (query:__all__)", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesVerifyQueryWithWildCardStar() throws IOException {
        String category = "My super duper favorite Men's category";
        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.add(CommonParams.Q, "*");
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(RuleManagerParams.CATEGORY_FILTER, category);
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        RuleManagerComponent mgr = new RuleManagerComponent();
        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND (query:__all__)", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesForCategoryPage() throws IOException {
        String category = "My category";
        RuleManagerComponent mgr = new RuleManagerComponent();

        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(RuleManagerParams.CATEGORY_FILTER, category);
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.category);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("target:allpages OR target:categorypages", filters.get(0));
        assertEquals("category:__all__ OR category:" + category, filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesVerifyQueryWithoutCategory() throws IOException {
        RuleManagerComponent mgr = new RuleManagerComponent();

        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(CommonParams.Q, "fantastic jackets");
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((fantastic jackets)^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__", filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesVerifyQueryWithBrandId() throws IOException {
        RuleManagerComponent mgr = new RuleManagerComponent();

        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.add(CommonParams.Q, "fantastic jackets");
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");
        requestParams.add(RuleManagerParams.BRAND_ID, "someBrand");

        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((fantastic jackets)^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__", filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__ OR brandId:someBrand", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Retail", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesVerifyQueryWithCloseout() throws IOException {
        RuleManagerComponent mgr = new RuleManagerComponent();

        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.set(FacetParams.FACET, true);
        requestParams.add(CommonParams.Q, "fantastic jackets");
        requestParams.add(RuleManagerParams.CATALOG_ID, "cata:alpha");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");
        requestParams.add(RuleManagerParams.BRAND_ID, "someBrand");
        requestParams.add(CommonParams.FQ, "isCloseout:true");

        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        List<String> filters = Arrays.asList(rulesQuery.getFilterQueries());
        assertEquals(7, filters.size());
        assertEquals("*:*", rulesQuery.getQuery());
        assertEquals("(target:allpages OR target:searchpages) AND ((fantastic jackets)^2 OR query:__all__)", filters.get(0));
        assertEquals("category:__all__", filters.get(1));
        assertEquals("siteId:__all__ OR siteId:site:alpha", filters.get(2));
        assertEquals("brandId:__all__ OR brandId:someBrand", filters.get(3));
        assertEquals("subTarget:__all__ OR subTarget:Outlet", filters.get(4));
        assertEquals("catalogId:__all__ OR catalogId:cata:alpha", filters.get(5));
        assertEquals("-(((startDate:[* TO *]) AND -(startDate:[* TO NOW/DAY+1DAY])) OR (endDate:[* TO *] AND -endDate:[NOW/DAY+1DAY TO *]))", filters.get(6));
    }

    @Test
    public void testLoadRulesNoCatalogId() throws IOException {
        RuleManagerComponent mgr = new RuleManagerComponent();

        ModifiableSolrParams requestParams = new ModifiableSolrParams();
        requestParams.add(CommonParams.Q, "fantastic jackets");
        requestParams.add(RuleManagerParams.SITE_IDS, "site:alpha");

        SolrQuery rulesQuery = mgr.getRulesQuery(requestParams, RuleManagerComponent.PageType.search);

        assertNull(rulesQuery);
    }
}
