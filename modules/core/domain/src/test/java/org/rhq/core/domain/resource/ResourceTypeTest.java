package org.rhq.core.domain.resource;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.testng.annotations.Test;
import org.unitils.UnitilsTestNG;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.orm.jpa.annotation.JpaEntityManagerFactory;

import static org.rhq.core.domain.resource.ResourceType.*;
import static org.testng.Assert.*;

@JpaEntityManagerFactory(persistenceUnit = "rhq-test", configFile = "META-INF/test-persistence.xml")
@DataSet
public class ResourceTypeTest extends UnitilsTestNG {

    @PersistenceContext
    EntityManager entityMgr;

    @Test
    @SuppressWarnings("unchecked")
    public void findByPlugin() {
        List<ResourceType> results = entityMgr.createNamedQuery(QUERY_FIND_BY_PLUGIN)
            .setParameter("plugin", "TestPlugin1")
            .getResultList();

        assertEquals(results.size(), 1, "Expected to get back 1 resource type");
        assertEquals(results.get(0).getId(), -1, "The wrong resource type was returned");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void findByNameAndPlugin() {
        ResourceType type = (ResourceType) entityMgr.createNamedQuery(QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", "TestServer1")
            .setParameter("plugin", "TestPlugin1")
            .getSingleResult();

        assertEquals(type.getId(), -1, "Failed to find resource type by name and by plugin");
    }

    @Test
    public void findByNameAndPluginShouldNotReturnDeletedType() {
        List results = entityMgr.createNamedQuery(QUERY_FIND_BY_NAME_AND_PLUGIN)
            .setParameter("name", "TestServer2")
            .setParameter("plugin", "TestPlugin1")
            .getResultList();

        assertEquals(results.size(), 0, "Deleted type should be ignored");
    }

}
