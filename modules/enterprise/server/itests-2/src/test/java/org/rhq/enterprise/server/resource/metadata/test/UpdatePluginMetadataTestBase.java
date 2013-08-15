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

import javax.persistence.NoResultException;
import javax.transaction.Status;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.drift.DriftServerPluginService;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.test.TransactionCallbackReturnable;
import org.rhq.enterprise.server.util.LookupUtil;

public class UpdatePluginMetadataTestBase extends AbstractEJB3Test {

    // this must match the constant found in ServerManagerBean
    protected static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";
    protected static final String RHQ_SERVER_NAME_PROPERTY_VALUE = "TestServer";

    protected TestServerCommunicationsService agentServiceContainer;

    protected static final String PLUGIN_NAME = "UpdatePluginMetadataTestBasePlugin"; // don't change this - our test descriptor .xml files use it as plugin name
    protected static final String AGENT_NAME = "-dummy agent-";
    protected static final String COMMON_PATH_PREFIX = "./test/metadata/";

    protected static PluginManagerLocal pluginMgr;
    protected static ResourceTypeManagerLocal resourceTypeManager;
    protected static ResourceManagerLocal resourceManager;
    private Server server;

    @Override
    protected void beforeMethod() throws Exception {
        agentServiceContainer = prepareForTestAgents();
        prepareMockAgentServiceContainer();
        prepareScheduler();
        preparePluginScannerService();
        // we perform lookups of config settings from SystemManagerBean.
        // SystemManagerBean.getDriftServerPluginManager method requires drift server plugin. 
        DriftServerPluginService driftServerPluginService = new DriftServerPluginService(getTempDir());
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        pluginMgr = LookupUtil.getPluginManager();
        resourceTypeManager = LookupUtil.getResourceTypeManager();
        resourceManager = LookupUtil.getResourceManager();

        System.setProperty(RHQ_SERVER_NAME_PROPERTY, RHQ_SERVER_NAME_PROPERTY_VALUE);
    }

    @Override
    protected void afterMethod() throws Exception {
        cleanupTest();

        unprepareServerPluginService();
        unpreparePluginScannerService();
        unprepareScheduler();
        unprepareForTestAgents();

        deleteServerIdentity();

        System.setProperty(RHQ_SERVER_NAME_PROPERTY, "");
    }

    protected void prepareMockAgentServiceContainer() {
        agentServiceContainer.measurementService = new MockMeasurementAgentService();
    }

    protected ResourceType getResourceType(String typeName) {
        return getResourceType(typeName, PLUGIN_NAME);
    }

    protected ResourceType getResourceType(String typeName, String pluginName) {
        Subject overlord = getOverlord();

        ResourceTypeCriteria resourceTypeCriteria = new ResourceTypeCriteria();
        resourceTypeCriteria.setStrict(true);
        resourceTypeCriteria.addFilterIgnored(null);
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
        Subject overlord = getOverlord();

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
        testPlugin.setDisplayName(PLUGIN_NAME + ": " + pathToDescriptor);
        PluginDescriptor descriptor = loadPluginDescriptor(pathToDescriptor);
        // if caller passed in their own version, use it - otherwise, use the plugin descriptor version.
        // this allows our tests to reuse descriptors without having to duplicate them
        if (versionOverride != null) {
            testPlugin.setVersion(versionOverride);
        } else {
            testPlugin.setVersion(descriptor.getVersion());
        }
        try {
            pluginMgr.registerPlugin(testPlugin, descriptor, null, true);
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

    protected int getPluginId() throws NoResultException {
        Plugin existingPlugin;
        try {
            existingPlugin = (Plugin) em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME).setParameter("name", PLUGIN_NAME)
                .getSingleResult();
            int plugin1Id = existingPlugin.getId();
            return plugin1Id;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected int getAgentId() throws NoResultException {
        try {
            Agent existingAgent = getAgent();
            int agentId = existingAgent.getId();
            return agentId;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected Agent getAgent() throws NoResultException {
        Agent existingAgent;
        try {
            existingAgent = (Agent) em.createNamedQuery(Agent.QUERY_FIND_BY_NAME).setParameter("name", AGENT_NAME)
                .getSingleResult();
            return existingAgent;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected void setUpAgent(Resource testResource) {
        Agent agent;
        try {
            agent = getAgent();
        } catch (NoResultException nre) {
            agent = new Agent(AGENT_NAME, "localhost", 12345, "http://localhost:12345/", "-dummy token-");
            em.persist(agent);
        }

        testResource.setAgent(agent);
        agentServiceContainer.addStartedAgent(agent);
    }

    protected void createServerIdentity() {
        server = new Server();
        server.setName(RHQ_SERVER_NAME_PROPERTY_VALUE);
        server.setAddress("localhost");
        server.setPort(7080);
        server.setSecurePort(7443);
        server.setComputePower(1);
        server.setOperationMode(Server.OperationMode.MAINTENANCE);
        int serverId = LookupUtil.getServerManager().create(server);
        assert serverId > 0 : "could not create our server identity in the DB";

        // simulate the agent being "connected" to the server
        try {
            Agent agent = getAgent();
            agent.setServer(server);
            LookupUtil.getAgentManager().updateAgent(agent);
        } catch (NoResultException nre) {
            // no agent to attach
        }
    }

    protected void deleteServerIdentity() throws Exception {
        if (server != null) {
            cleanupAgent(); // can't remove the server before we purge the agent
            LookupUtil.getTopologyManager().deleteServer(LookupUtil.getSubjectManager().getOverlord(), server.getId());
            server = null;
        }
    }

    /**
     * A dummy that needs to be set up before running ResourceManager.deleteResource()
     */
    public class MockMeasurementAgentService implements MeasurementAgentService {

        @Override
        public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, Set<MeasurementScheduleRequest> requests) {
            return null;
        }

        public void scheduleCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
        }

        public void unscheduleCollection(Set<Integer> resourceIds) {
        }

        public void updateCollection(Set<ResourceMeasurementScheduleRequest> resourceSchedules) {
        }

        public Map<String, Object> getMeasurementScheduleInfoForResource(int resourceId) {
            return null;
        }
    }

    protected void cleanupTest() throws Exception {
        cleanupMetadata();
        cleanupAgent();
        cleanupPlugin();
    }

    protected void cleanupMetadata() throws Exception {
        String pathToDescriptor = COMMON_PATH_PREFIX + "/noTypes.xml";
        registerPluginInternal(pathToDescriptor, null);
    }

    protected void cleanupAgent() throws Exception {
        getTransactionManager().begin();

        try {
            try {
                int agentId = getAgentId();
                Agent agent = em.getReference(Agent.class, agentId);
                if (null != agent) {
                    em.remove(agent);
                }
            } catch (NoResultException nre) {
                // ignore, nothing to clean up
            }
        } catch (Exception e) {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            System.out.println("CANNOT CLEAN UP AGENT: " + e);
            throw e;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().commit();
            }
        }
    }

    protected void cleanupPlugin() throws Exception {
        getTransactionManager().begin();

        try {
            try {
                int plugin1Id = getPluginId();
                Plugin plugin1 = em.getReference(Plugin.class, plugin1Id);
                if (null != plugin1) {
                    em.remove(plugin1);
                }
            } catch (NoResultException nre) {
                // ignore, nothing to do
            }
        } catch (Exception e) {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().rollback();
            }
            System.out.println("CANNOT CLEAN UP PLUGIN: " + e);
            throw e;
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
                Subject overlord = getOverlord();
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
            System.out.println("CANNOT CLEAN UP RESOURCE TYPE: " + rtName + ": " + e);
            throw e;
        } finally {
            if (Status.STATUS_NO_TRANSACTION != getTransactionManager().getStatus()) {
                getTransactionManager().commit();
            }
        }
    }

    //
    // provide some convenience methods to create resources
    //

    protected Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    protected Resource persistNewResource(final String resourceTypeName) throws Exception {
        return executeInTransaction(false, new TransactionCallbackReturnable<Resource>() {

            public Resource execute() throws Exception {
                ResourceType resourceType = getResourceType(resourceTypeName);
                Resource resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setUuid(UUID.randomUUID().toString());
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                setUpAgent(resource);
                em.persist(resource);
                return resource;
            }
        });
    }

    protected void deleteNewResource(final Resource resource) throws Exception {
        try {
            List<Integer> deletedIds = resourceManager.uninventoryResource(getOverlord(), resource.getId());
            for (Integer deletedResourceId : deletedIds) {
                resourceManager.uninventoryResourceAsyncWork(getOverlord(), deletedResourceId);
            }
        } catch (Exception e) {
            System.out.println("CANNOT CLEAN UP RESOURCE: " + resource + ": " + e);
            throw e;
        }
    }

}
