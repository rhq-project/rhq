/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.resource.metadata.test;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataRequest;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

// TODO: Fix typo in class name.
public class UpdateSubsytemTestBase extends AbstractEJB3Test {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    @EJB
    protected ResourceManagerLocal resMgr;

    protected TestServerCommunicationsService agentServiceContainer;

    protected int agentId;
    protected int plugin1Id;

    protected static final String PLUGIN_NAME = "ResourceMetaDataManagerBeanTest";
    protected static final String AGENT_NAME = "-dummy agent-";
    protected static final String COMMON_PATH_PREFIX = "./test/metadata/";

    protected static ResourceMetadataManagerLocal metadataManager;
    protected static ResourceTypeManagerLocal resourceTypeManager;
    protected static ResourceManagerLocal resourceManager;

    @BeforeSuite
    protected void init() {
        try {
            metadataManager = LookupUtil.getResourceMetadataManager();
            resourceTypeManager = LookupUtil.getResourceTypeManager();
            resourceManager = LookupUtil.getResourceManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    @BeforeClass
    public void beforeClass() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.measurementService = new MockAgentService();

        prepareScheduler();
    }

    @AfterClass
    public void afterClass() throws Exception {
        unprepareForTestAgents();
        unprepareScheduler();
    }

    @BeforeTest
    public void beforeTest() throws Exception {
        startTest();
        // cleanup anything left over from a previous, interrupted run.
        cleanupTest();
    }

    protected ResourceType getResourceType(String typeName) {
        return getResourceType(typeName, PLUGIN_NAME);
    }

    protected ResourceType getResourceType(String typeName, String pluginName) {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ResourceTypeCriteria resourceTypeCriteria = new ResourceTypeCriteria();
        resourceTypeCriteria.setStrict(true);
        resourceTypeCriteria.addFilterName(typeName);
        resourceTypeCriteria.addFilterPluginName(pluginName);

        // used in UpdateMeasurementSubsystemTest
        resourceTypeCriteria.fetchMetricDefinitions(true);
        // used in several UpdateResourceSubsystemTest tests
        resourceTypeCriteria.fetchSubCategory(true);

        PageList<ResourceType> results = resourceTypeManager
            .findResourceTypesByCriteria(overlord, resourceTypeCriteria);
        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new IllegalStateException("Found more than one resourceType with name " + typeName + " from plugin "
                + pluginName + ".");
        }
    }

    protected Resource createResource(String resourceKey, String name, ResourceType type) {
        Resource resource = new Resource(resourceKey, name, type);
        resource.setUuid(UUID.randomUUID().toString());
        resource.setInventoryStatus(InventoryStatus.COMMITTED);
        return resource;
    }

    protected Resource getResource(ResourceCriteria resourceCriteria) {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        PageList<Resource> results = resourceManager.findResourcesByCriteria(overlord, resourceCriteria);
        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new IllegalStateException("Found more than one Resource with criteria " + resourceCriteria + ".");
        }
    }

    protected String getSubsystemDirectory() {
        return ".";
    }

    protected void registerPlugin(String pathToDescriptor, String versionOverride) throws Exception {
        pathToDescriptor = COMMON_PATH_PREFIX + getSubsystemDirectory() + "/" + pathToDescriptor;
        registerPluginInternal(pathToDescriptor, versionOverride);
    }

    private void registerPluginInternal(String pathToDescriptor, String versionOverride) throws Exception {
        System.out.println("Registering plugin with descriptor [" + pathToDescriptor + "]...");
        String md5 = MessageDigestGenerator.getDigestString(pathToDescriptor);
        Plugin testPlugin = new Plugin(PLUGIN_NAME, "foo.jar", md5);
        testPlugin.setDisplayName("ResourceMetaDataManagerBeanTest: " + pathToDescriptor);
        PluginDescriptor descriptor = loadPluginDescriptor(pathToDescriptor);
        // if caller passed in their own version, use it - otherwise, use the plugin descriptor version.
        // this allows our tests to reuse descriptors without having to duplicate them
        if (versionOverride != null) {
            testPlugin.setVersion(versionOverride);
        } else {
            testPlugin.setVersion(descriptor.getVersion());
        }
        try {
            metadataManager.registerPlugin(LookupUtil.getSubjectManager().getOverlord(), testPlugin, descriptor, null,
                    true);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }

    }

    protected void registerPlugin(String pathToDescriptor) throws Exception {
        registerPlugin(pathToDescriptor, null);
    }

    public PluginDescriptor loadPluginDescriptor(String descriptorFile) throws Exception {
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        URL pluginSchemaURL = this.getClass().getClassLoader().getResource("rhq-plugin.xsd");
        Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);
        unmarshaller.setSchema(pluginSchema);
        if (descriptorUrl == null)
            throw new FileNotFoundException("File " + descriptorFile + " not found");
        return (PluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
    }

    protected boolean containedIn(String string, String[] references) {
        boolean found = false;
        for (String ref : references) {
            if (string.equals(ref)) {
                found = true;
                break;
            }
        }

        return found;
    }

    protected int getPluginId(EntityManager entityManager) {
        Plugin existingPlugin;
        try {
            existingPlugin = (Plugin) entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME).setParameter("name",
                PLUGIN_NAME).getSingleResult();
            plugin1Id = existingPlugin.getId();
            return plugin1Id;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected int getAgentId(EntityManager entityManager) {
        Agent existingAgent;
        try {
            existingAgent = (Agent) entityManager.createNamedQuery(Agent.QUERY_FIND_BY_NAME).setParameter("name",
                AGENT_NAME).getSingleResult();
            agentId = existingAgent.getId();
            return agentId;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected void setUpAgent(EntityManager entityManager, Resource testResource) {
        Agent agent = new Agent(AGENT_NAME, "localhost", 12345, "http://localhost:12345/", "-dummy token-");
        entityManager.persist(agent);

        testResource.setAgent(agent);
        agentServiceContainer.addStartedAgent(agent);
        agentId = agent.getId();
    }

    /**
     * A dummy that needs to be set up before running ResourceManager.deleteResource()
     */
    public class MockAgentService implements MeasurementAgentService {

        @Override
        public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, List<MeasurementDataRequest> requests) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void scheduleCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
            // TODO Auto-generated method stub

        }

        public void unscheduleCollection(Set<Integer> resourceIds) {
            // TODO Auto-generated method stub

        }

        public void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
            // TODO Auto-generated method stub

        }

        public Map<String, Object> getMeasurementScheduleInfoForResource(int resourceId) {
            return null;
        }
    }

    protected void cleanupTest() throws Exception {
        try {
            String pathToDescriptor = COMMON_PATH_PREFIX + "/noTypes.xml";
            registerPluginInternal(pathToDescriptor, null);

            /*cleanupResourceType("constraintPlatform");
            cleanupResourceType("events");
            cleanupResourceType("groupDeletedPlatform");
            cleanupResourceType("groupPropDeletedPlatform");
            cleanupResourceType("groupPropMovedPlatform");
            cleanupResourceType("myPlatform");
            cleanupResourceType("myPlatform3");
            cleanupResourceType("myPlatform4");
            cleanupResourceType("myPlatform5");
            cleanupResourceType("myPlatform6");
            cleanupResourceType("myPlatform7");
            cleanupResourceType("ops");
            cleanupResourceType("testApp1");
            cleanupResourceType("testServer1");
            cleanupResourceType("testServer2");
            cleanupResourceType("testService1");
            cleanupResourceType("testService2");
            cleanupResourceType("TestServer");*/

            getTransactionManager().begin();
            EntityManager entityManager = getEntityManager();

            try {
                agentId = getAgentId(entityManager);
                Agent agent = entityManager.getReference(Agent.class, agentId);
                if (null != agent) {
                    entityManager.remove(agent);
                }
            } catch (Exception e) {
                // ignore, agent does not exist
            }

            try {
                plugin1Id = getPluginId(entityManager);
                Plugin plugin1 = entityManager.getReference(Plugin.class, plugin1Id);
                if (null != plugin1) {
                    entityManager.remove(plugin1);
                }
            } catch (Exception e) {
                // ignore, plugin1 does not exist
            }
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().commit();
            }
        }
    }

    protected void cleanupResourceType(String rtName) throws Exception {
        try {
            ResourceType rt = getResourceType(rtName);

            if (null != rt) {
                Subject overlord = LookupUtil.getSubjectManager().getOverlord();
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

                // delete any resources first
                ResourceCriteria c = new ResourceCriteria();
                c.setStrict(true);
                c.addFilterResourceTypeId(rt.getId());
                c.addFilterInventoryStatus(InventoryStatus.NEW);
                List<Resource> doomedResources = resourceManager.findResourcesByCriteria(overlord, c);
                c.addFilterInventoryStatus(InventoryStatus.DELETED);
                doomedResources.addAll(resourceManager.findResourcesByCriteria(overlord, c));
                c.addFilterInventoryStatus(InventoryStatus.UNINVENTORIED);
                doomedResources.addAll(resourceManager.findResourcesByCriteria(overlord, c));
                // invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
                // perform in-band and out-of-band work in quick succession
                for (Resource doomed : doomedResources) {
                    List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, doomed.getId());
                    for (Integer deletedResourceId : deletedIds) {
                        resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
                    }
                }

                // Measurement defs go away via cascade remove

                getTransactionManager().begin();
                EntityManager em = getEntityManager();

                rt = em.find(ResourceType.class, rt.getId());
                ResourceType parent = rt.getParentResourceTypes().isEmpty() ? null : rt.getParentResourceTypes()
                    .iterator().next();
                em.remove(rt);
                if (null != parent) {
                    em.remove(parent);
                }
            }
        } catch (Exception e) {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            System.out.println("CANNOT CLEAN UP TEST (cleanupResourceType): " + e);
            throw e;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().commit();
            }
        }
    }
}
