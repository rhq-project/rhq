package org.rhq.core.domain.resource;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.iterators.ArrayListIterator;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.orm.jpa.annotation.JpaEntityManagerFactory;

import static org.rhq.core.domain.resource.ResourceType.QUERY_FIND_ALL;
import static org.rhq.core.domain.resource.ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN;
import static org.rhq.core.domain.resource.ResourceType.QUERY_FIND_BY_PLUGIN;
import static org.rhq.core.domain.resource.ResourceType.QUERY_FIND_CHILDREN;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@JpaEntityManagerFactory(persistenceUnit = "rhq-test", configFile = "META-INF/test-persistence.xml")
@DataSet
public class ResourceTypeTest extends UnitilsTestNG {

    @BeforeClass(groups = "unitils", dependsOnGroups = "integration.ejb3")
    public void init() {
    }

    @PersistenceContext
    EntityManager entityMgr;

    @Test(groups = "unitils", dependsOnGroups = "integration.ejb3")
    @SuppressWarnings("unchecked")
    public void findByPlugin() {
        List<ResourceType> results = entityMgr.createNamedQuery(QUERY_FIND_BY_PLUGIN)
            .setParameter("plugin", "TestPlugin1")
            .getResultList();

        assertEquals(results.size(), 1, "Expected to get back 1 resource type");
        assertEquals(results.get(0).getId(), -1, "The wrong resource type was returned");
    }

    @Test(groups = "unitils", dependsOnGroups = "integration.ejb3")
    @SuppressWarnings("unchecked")
    public void findByNameAndPlugin() {
        ResourceType type = (ResourceType) entityMgr.createNamedQuery(QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", "TestServer1")
            .setParameter("plugin", "TestPlugin1")
            .getSingleResult();

        assertEquals(type.getId(), -1, "Failed to find resource type by name and by plugin");
    }

    @Test(groups = "unitils", dependsOnGroups = "integration.ejb3")
    public void findByNameAndPluginShouldNotReturnDeletedType() {
        List results = entityMgr.createNamedQuery(QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", "TestServer2")
            .setParameter("plugin", "TestPlugin1")
            .getResultList();

        assertEquals(results.size(), 0, "Deleted types should be ignored");
    }

    @Test(groups = "unitils", dependsOnGroups = "integration.ejb3")
    @SuppressWarnings("unchecked")
    public void findAll() {
        List<ResourceType> results = entityMgr.createNamedQuery(QUERY_FIND_ALL).getResultList();

        assertTrue(results.size() > 0, "Expected a non-empty result set");
        assertDeletedTypesIgnored(results);
    }

    @Test(groups = "unitils", dependsOnGroups = "integration.ejb3")
    @SuppressWarnings("unchecked")
    public void findChildren() {
        List<ResourceType> results = entityMgr.createNamedQuery(QUERY_FIND_CHILDREN)
            .setParameter("resourceTypeId", -4)
            .getResultList();

        assertEquals(results.size(), 2, "Expected to get two resource types for resource type with id -4");
        assertDeletedTypesIgnored(results);
    }

    void assertDeletedTypesIgnored(List<ResourceType> types) {
        List<ResourceType> deletedTypes = new ArrayList<ResourceType>();

        for (ResourceType type : types) {
            if (type.isDeleted()) {
                deletedTypes.add(type);
            }
        }
        assertTrue(deletedTypes.size() == 0, "Deleted types should be ignored. The following deleted types were " +
            "found: " + deletedTypes);
    }

}
