package org.rhq.enterprise.server.inventory;

import org.apache.poi.hssf.record.formula.functions.Lookup;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class InventoryManagerBeanTest extends AbstractEJB3Test {

    @Test
    public void deleteResourceTypes() throws Exception {
        initDB();

        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        inventoryMgr.markTypesDeleted(asList(1, 4));
    }

    @SuppressWarnings("unchecked")
    @Test(dependsOnMethods = {"deleteResourceTypes"})
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
    @Test(dependsOnMethods = {"deleteResourceTypes"})
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

    public void initDB() throws Exception {
        IDatabaseConnection conn = new DatabaseConnection(getConnection());
        setDbType(conn);
        DatabaseOperation.CLEAN_INSERT.execute(conn, getDataSet());
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
