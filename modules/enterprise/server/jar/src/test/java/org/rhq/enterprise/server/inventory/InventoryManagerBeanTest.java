package org.rhq.enterprise.server.inventory;

import static java.util.Arrays.asList;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class InventoryManagerBeanTest extends AbstractEJB3Test {

    @BeforeClass
    public void deleteResourceTypes() throws Exception {
        initDB();

        getTransactionManager().begin();
        List<Integer> resourceTypeIds = asList(1, 2, 3, 4, 5);
        
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        inventoryMgr.markTypesDeleted(resourceTypeIds);
        getTransactionManager().commit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void markTypesAndTheirChildTypesForDeletion() {
        List<ResourceType> resourceTypes = getEntityManager().createQuery(
                "from ResourceType t where t.id in (:resourceTypeIds)")
                .setParameter("resourceTypeIds", asList(1, 2, 3, 4, 5))
                .getResultList();

        assertEquals("Failed to retrieve all resource types", 5, resourceTypes.size());

        List<Integer> typesNotDeleted = new ArrayList<Integer>();
        for (ResourceType type : resourceTypes) {
           if (!type.isDeleted()) {
               typesNotDeleted.add(type.getId());
           }
        }

        assertEquals(
                "Failed to mark for deletion resource types with the following ids: " + typesNotDeleted + ".",
                0,
                typesNotDeleted.size()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void uninventoryResourcesOfTypesMarkedForDeletion() {
        List<Resource> resources = getEntityManager().createQuery("from Resource r where r.id in (:resourceIds)")
                .setParameter("resourceIds", asList(1, 2))
                .getResultList();

        assertEquals("Failed to retrieve all resources", 2, resources.size());

        List<Integer> resourcesNotDeleted = new ArrayList<Integer>();
        for (Resource resource : resources) {
            if (resource.getInventoryStatus() != InventoryStatus.UNINVENTORIED) {
                resourcesNotDeleted.add(resource.getId());
            }
        }

        assertEquals(
                "Resources of types marked for deletion should be uninventoried. Resources with the following ids " +
                        "should have been uninventoried: " + resourcesNotDeleted + ".",
                0,
                resourcesNotDeleted.size()
        );
    }

    @Test
    public void getDeletedTypes() {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        List<ResourceType> deletedTypes = inventoryMgr.getDeletedTypes();

        assertEquals("Expected to get back five deleted resource types", 5, deletedTypes.size());
    }

    @Test
    public void deletedTypeIsReadyForRemovalWhenThereResourcesOfThatType() {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        ResourceType deletedType = getEntityManager().find(ResourceType.class, 4);

        assertTrue(deletedType + " should be ready for removal", inventoryMgr.isReadyForPermanentRemoval(deletedType));
    }

    @Test
    public void typeThatIsNotDeletedIsNotReadyForRemoval() {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        ResourceType resourceType = getEntityManager().find(ResourceType.class, 6);

        assertFalse(resourceType + " is not ready for removal because it is not deleted.",
                inventoryMgr.isReadyForPermanentRemoval(resourceType));
    }

    public void initDB() throws Exception {
        Connection connection = null;

        try {
            connection = getConnection();
            IDatabaseConnection conn = new DatabaseConnection(connection);
            setDbType(conn);
            DatabaseOperation.CLEAN_INSERT.execute(conn, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    void setDbType(IDatabaseConnection connection) throws Exception {
        DatabaseConfig dbConfig = connection.getConfig();
        String name = connection.getConnection().getMetaData().getDatabaseProductName().toLowerCase();
        int major = connection.getConnection().getMetaData().getDatabaseMajorVersion();
        IDataTypeFactory type = null;

        if (name.contains("postgres")) {
            type = new PostgresqlDataTypeFactory();
        } else if (name.contains("oracle")) {
            if (major >= 10) {
                type = new Oracle10DataTypeFactory();
            } else {
                type = new OracleDataTypeFactory();
            }
        }

        if (type != null) {
            dbConfig.setProperty("http://www.dbunit.org/properties/datatypeFactory",type);
        }
    }

    IDataSet getDataSet() throws Exception {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(
                getDataSetFile())));
        xmlProducer.setColumnSensing(true);
        return new FlatXmlDataSet(xmlProducer);
    }

    String getDataSetFile() {
        return getClass().getSimpleName() + ".xml";
    }

}
