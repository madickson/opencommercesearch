package org.opencommercesearch.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import atg.adapter.gsa.GSARepository;
import atg.commerce.inventory.InventoryException;
import atg.commerce.inventory.InventoryManager;
import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;
import atg.repository.Repository;

/**
 * This class implements a sequential in-memory inventory manager to help speeding up the feed
 * process. This inventory manager doesn't implement all the methods provided by the
 * interface, but only the methods required by search feed. This functionality can
 * be customized by tweaking the SQL query properties.
 *
 * The inventory manager assumes the sku ids are queried in order. When a sku with an id greater
 * to the last sku id in the cache is accessed the component will discard all the items in cache and load
 * the requested skus and n successor skus.
 *
 * The manager also allows going back and forth. However, going back in the list will require more
 * repository requests.
 *
 * The class is not thread safe.
 *
 * @TODO currently loads stock level, make it customizable so subclasses can load other inventory properties (e.g. status)
 */
public class SequentialInMemoryInventoryManager extends GenericService implements InventoryManager {

    public static final String LOCALE_SEPARATOR = ":";

    private Map<String, Long> inventoryMap = null;
    private String inventoryName = "In Memory Inventory";
    private String inventorySql;
    private int batchSize = 10000;
    private Repository inventoryRepository;
    private String minSkuId = null;
    private String maxSkuId = null;

    public String getInventorySql() {
        return inventorySql;
    }

    public void setInventorySql(String inventorySql) {
        this.inventorySql = inventorySql;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Repository getInventoryRepository() {
        return inventoryRepository;
    }

    public void setInventoryRepository(Repository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void doStartService() throws ServiceException {
        super.doStartService();
        if (getInventorySql() == null) {
            throw new ServiceException("Inventory count SQL is required");
        }
        if (getInventoryRepository() == null) {
            throw new ServiceException("Inventory repository is required");
        }
    }

    /**
     * Loads the inventory items into a hash map
     */
    void loadInventory(String id) {
        Connection connection = null;
        try {
            connection = ((GSARepository) getInventoryRepository()).getDataSource().getConnection();
            PreparedStatement countStmt = null;
            PreparedStatement inventoryStmt = null;
            try {
                inventoryStmt = connection.prepareStatement(getInventorySql());
                loadInventory(inventoryStmt, id);
            } catch (SQLException ex) {
                if (isLoggingError()) {
                    logError("Could not load inventory into memory", ex);
                }
            } finally {
                try {
                    if (null != countStmt) {
                        countStmt.close();
                    }
                    if (null != inventoryStmt) {
                        inventoryStmt.close();
                    }
                } catch (SQLException ex) {
                    if (isLoggingError()) {
                        logError("Could not close prepared statements ", ex);
                    }
                }
            }
        } catch (SQLException ex) {
            if (isLoggingError()) {
                logError("Could not connect to the database", ex);
            }
        } finally {
            try {
                if (null != connection) {
                    connection.close();
                }
            } catch (SQLException ex) {
                if (isLoggingError()) {
                    logError("Could not close database connection ", ex);
                }
            }
        }
    }


    /**
     *  Just a helper method to load inventory items
     */
    private void loadInventory(PreparedStatement inventoryStmt, String id) throws SQLException {
        if (inventoryMap == null) {
            inventoryMap = new HashMap<String, Long>(batchSize);
        } else {
            inventoryMap.clear();
        }

        long startTime = System.currentTimeMillis();
        if (isLoggingDebug()) {
            logDebug("Loading " + id + " + " +  getBatchSize() + " successors");
        }

        int offset = 1;
        int hits = getBatchSize();

        inventoryStmt.setString(1, id);
        inventoryStmt.setInt(2, offset);
        inventoryStmt.setInt(3, hits);
        minSkuId = id;

        if (inventoryStmt.execute()) {
            ResultSet rs = inventoryStmt.getResultSet();

            while (rs.next()) {
                String skuId = rs.getString("catalog_ref_id");
                inventoryMap.put(skuId, rs.getLong("stock_level"));
                maxSkuId = skuId;
            }

            try {
                rs.close();
            } catch (SQLException ex) {
                if (isLoggingError()) {
                    logError("Error result set", ex);
                }
            }
        }

        if (isLoggingInfo()) {
            logInfo("Building map finished in " + ((System.currentTimeMillis() - startTime) / 1000)
                    + " seconds. Sku range: " + minSkuId + " - " + maxSkuId + ". Inventory contains "
                    + inventoryMap.size() + " items");
        }
    }

    @Override
    public void acquireInventoryLocks(List itemIds) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int backorder(String id, long howMany) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decreaseBackorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decreasePreorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decreaseStockLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInventoryName() {
        return inventoryName;
    }

    public void setInventoryName(String inventoryName) {
        this.inventoryName = inventoryName;
    }

    @Override
    public int increaseBackorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int increasePreorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int increaseStockLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int inventoryWasUpdated(List id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int preorder(String id, long howMany) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int purchase(String id, long howMany) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int purchaseOffBackorder(String id, long howMany) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int purchaseOffPreorder(String id, long howMany) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date queryAvailabilityDate(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int queryAvailabilityStatus(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long queryBackorderLevel(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long queryBackorderThreshold(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long queryPreorderLevel(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long queryPreorderThreshold(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long queryStockLevel(String id) throws InventoryException {
        int localeSeparatorIndex = id.lastIndexOf(LOCALE_SEPARATOR);
        if (localeSeparatorIndex != -1) {
            id = id.substring(0, localeSeparatorIndex);
        }

        if (maxSkuId == null || id.compareTo(minSkuId) < 0 || id.compareTo(maxSkuId) > 0) {
            loadInventory(id);
        }

        Long stockLevel = inventoryMap.get(id);

        if (stockLevel == null) {
            throw new InventoryException("Inventory not found for " + id);
        }

        if (isLoggingDebug()) {
            logDebug("Stock level for " + id + " is " + stockLevel);
        }

        return stockLevel;
    }

    @Override
    public long queryStockThreshold(String id) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releaseInventoryLocks(List itemIds) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setAvailabilityDate(String id, Date date) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setAvailabilityStatus(String id, int status) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBackorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBackorderThreshold(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setPreorderLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setPreorderThreshold(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setStockLevel(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setStockThreshold(String id, long number) throws InventoryException {
        throw new UnsupportedOperationException();
    }

}