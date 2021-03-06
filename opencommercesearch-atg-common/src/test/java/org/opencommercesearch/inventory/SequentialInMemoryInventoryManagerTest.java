package org.opencommercesearch.inventory;

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

import atg.adapter.gsa.GSARepository;
import atg.commerce.inventory.InventoryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;

/**
 * @author rmerizalde
 **/
public class SequentialInMemoryInventoryManagerTest {

    @Mock
    private GSARepository inventoryRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement stmt;

    @Mock
    private ResultSet rs;

    @Spy
    SequentialInMemoryInventoryManager manager = new SequentialInMemoryInventoryManager();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        manager.setSqlQuery("select 'dummy' from dual");
        manager.setRepository(inventoryRepository);
        manager.doStartService();
        when(inventoryRepository.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.execute()).thenReturn(true);
        when(stmt.getResultSet()).thenReturn(rs);

        final AtomicInteger index = new AtomicInteger();
        final StringBuffer id = new StringBuffer();
        final HashMap map = new HashMap();
        map.put("SKU0001-01", new String[] {"SKU0001-01", "SKU0001-02", "SKU0002-01", "SKU0002-02"});
        map.put("SKU0002-03", new String[] {"SKU0002-03", "SKU0003-03", "SKU0003-04", "SKU0004-01"});
        map.put("SKU0004-02", new String[] {"SKU0004-02", "SKU0004-03", "SKU0004-04", "SKU0004-05"});


        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String skuId = (String) args[1];
                id.setLength(0);
                id.append(skuId);
                index.set(-1);
                return null;
            }
        }).when(stmt).setObject(eq(1), anyString());

        when(rs.next()).then(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                String[] ids = (String[]) map.get(id.toString());

                if (ids != null) {
                    return index.incrementAndGet() < ids.length;
                }

                return false;
            }
        });

        when(rs.getObject("id")).then(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                String[] ids = (String[]) map.get(id.toString());
                return ids[index.get()];
            }
        });

        when(rs.getLong("stock_level")).then(new Answer<Long>() {
            public Long answer(InvocationOnMock invocation) throws Throwable {

                String[] ids = (String[]) map.get(id.toString());
                String next = ids[index.get()];
                return Long.parseLong(next.substring(next.length() - 1, next.length()));
            }
        });

    }


    @Test
    public void testQueryStockLevel() throws Exception {
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));
        assertEquals(2, manager.queryStockLevel("SKU0001-02"));
        assertEquals(1, manager.queryStockLevel("SKU0002-01"));
        assertEquals(2, manager.queryStockLevel("SKU0002-02"));
        assertEquals(3, manager.queryStockLevel("SKU0002-03"));
        assertEquals(3, manager.queryStockLevel("SKU0003-03"));
        assertEquals(4, manager.queryStockLevel("SKU0003-04"));
        assertEquals(1, manager.queryStockLevel("SKU0004-01"));
        assertEquals(2, manager.queryStockLevel("SKU0004-02"));
        assertEquals(3, manager.queryStockLevel("SKU0004-03"));
        assertEquals(4, manager.queryStockLevel("SKU0004-04"));
        assertEquals(5, manager.queryStockLevel("SKU0004-05"));
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testQueryStockLevelMissingInventory() throws Exception {
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));
        assertEquals(2, manager.queryStockLevel("SKU0001-02"));
        assertEquals(1, manager.queryStockLevel("SKU0002-01"));
        assertEquals(2, manager.queryStockLevel("SKU0002-02"));
        assertEquals(3, manager.queryStockLevel("SKU0002-03"));
        assertEquals(3, manager.queryStockLevel("SKU0003-03"));
        assertEquals(4, manager.queryStockLevel("SKU0003-04"));
        assertEquals(1, manager.queryStockLevel("SKU0004-01"));
        assertEquals(2, manager.queryStockLevel("SKU0004-02"));
        assertEquals(3, manager.queryStockLevel("SKU0004-03"));
        assertEquals(4, manager.queryStockLevel("SKU0004-04"));
        assertEquals(5, manager.queryStockLevel("SKU0004-05"));
        queryStockLevel("SKU0004-06");
        queryStockLevel("SKU0004-07");
        queryStockLevel("SKU0004-08");
        queryStockLevel("SKU0004-09");
        verify(stmt, times(4)).execute();
    }

    @Test(expected = InventoryException.class)
    public void testQueryStockLevelNotFound() throws Exception {

        assertEquals(5, manager.queryStockLevel("SKU0006-05"));
        verify(stmt, times(1)).execute();
    }

    @Test
    public void testQueryStockLevelBackAndForth() throws Exception {
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));
        assertEquals(2, manager.queryStockLevel("SKU0001-02"));
        assertEquals(1, manager.queryStockLevel("SKU0002-01"));
        assertEquals(2, manager.queryStockLevel("SKU0002-02"));
        assertEquals(3, manager.queryStockLevel("SKU0002-03"));
        assertEquals(3, manager.queryStockLevel("SKU0003-03"));
        assertEquals(4, manager.queryStockLevel("SKU0003-04"));
        assertEquals(1, manager.queryStockLevel("SKU0004-01"));
        assertEquals(2, manager.queryStockLevel("SKU0004-02"));
        assertEquals(3, manager.queryStockLevel("SKU0004-03"));
        assertEquals(4, manager.queryStockLevel("SKU0004-04"));
        assertEquals(5, manager.queryStockLevel("SKU0004-05"));
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));
        assertEquals(2, manager.queryStockLevel("SKU0004-02"));
        assertEquals(3, manager.queryStockLevel("SKU0002-03"));

        verify(stmt, times(6)).execute();
    }

    @Test
    public void testQueryStockLevelBackAndForthMissingInventory() throws Exception {
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));
        assertEquals(2, manager.queryStockLevel("SKU0001-02"));
        assertEquals(1, manager.queryStockLevel("SKU0002-01"));
        assertEquals(2, manager.queryStockLevel("SKU0002-02"));
        assertEquals(3, manager.queryStockLevel("SKU0002-03"));
        assertEquals(3, manager.queryStockLevel("SKU0003-03"));
        assertEquals(4, manager.queryStockLevel("SKU0003-04"));
        assertEquals(1, manager.queryStockLevel("SKU0004-01"));
        assertEquals(2, manager.queryStockLevel("SKU0004-02"));
        assertEquals(3, manager.queryStockLevel("SKU0004-03"));
        assertEquals(4, manager.queryStockLevel("SKU0004-04"));
        assertEquals(5, manager.queryStockLevel("SKU0004-05"));
        queryStockLevel("SKU0004-06");
        queryStockLevel("SKU0004-07");
        queryStockLevel("SKU0004-08");
        queryStockLevel("SKU0004-09");
        assertEquals(1, manager.queryStockLevel("SKU0001-01"));

        verify(stmt, times(4)).execute();
    }

    @Test
    public void testQueryStockLevelWithLocale() throws Exception {
        assertEquals(1, manager.queryStockLevel("SKU0001-01:US"));

        verify(stmt, times(1)).execute();
    }

    @Test
    public void testQueryStockLevelNoInventory() throws Exception {
        when(rs.next()).thenReturn(false);

        queryStockLevel("SKU0001-01");
        queryStockLevel("SKU0002-01");
        queryStockLevel("SKU0003-01");
        queryStockLevel("SKU0004-01");
        verify(stmt, times(1)).execute();
    }

    private long queryStockLevel(String productId) {
        try {
            return manager.queryStockLevel("SKU0001-01");
        } catch (Exception ex) {}
        return -1;
    }


}
