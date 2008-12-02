/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
import java.util.Set;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

public class UpdateSubsytemTestBase extends AbstractEJB3Test {

    @EJB
    protected ResourceManagerLocal resMgr;

    protected TestServerCommunicationsService agentServiceContainer;

    protected int agentId;
    protected int server2id;
    protected int plugin1Id;

    protected static final String PLUGIN_NAME = "ResourceMetaDataManagerBeanTest";
    protected static final String COMMON_PATH_PREFIX = "./test/metadata/";
    protected static ResourceMetadataManagerLocal metadataManager;

    @BeforeSuite
    protected void init() {
        try {
            metadataManager = LookupUtil.getResourceMetadataManager();
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

    @SuppressWarnings("unchecked")
    protected ResourceType getResourceType(String typeName) {
        Query q1 = getEntityManager().createQuery("Select rt from ResourceType rt");
        List<ResourceType> types = q1.getResultList();
        String lookup = "==== Lookup: " + typeName + "Found:\n" + types;

        Query q = getEntityManager().createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN);
        q.setParameter("name", typeName).setParameter("plugin", PLUGIN_NAME);
        try {
            ResourceType type = (ResourceType) q.getSingleResult();
            return type;
        } catch (NoResultException nre) {
            throw new NoResultException(lookup);
        }
    }

    protected String getSubsystemDirectory() {
        return ".";
    }

    protected void registerPlugin(String pathToDescriptor) throws Exception {
        pathToDescriptor = COMMON_PATH_PREFIX + getSubsystemDirectory() + "/" + pathToDescriptor;
        Plugin testPlugin = new Plugin(PLUGIN_NAME, "foo.jar", "123561RE1652EF165E");
        testPlugin.setDisplayName("ResourceMetaDataManagerBeanTest" + pathToDescriptor);
        PluginDescriptor descriptor = loadPluginDescriptor(pathToDescriptor);
        metadataManager.registerPlugin(testPlugin, descriptor);
        getEntityManager().flush();
    }

    public PluginDescriptor loadPluginDescriptor(String descriptorFile) throws Exception {
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PC_PLUGIN);
        URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
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
            existingPlugin = (Plugin) entityManager.createNamedQuery("Plugin.findByName").setParameter("name",
                PLUGIN_NAME).getSingleResult();
            plugin1Id = existingPlugin.getId();
            return plugin1Id;
        } catch (NoResultException nre) {
            throw nre;
        }
    }

    protected void setUpAgent(EntityManager entityManager, Resource testResource) {
        Agent agent = new Agent("-dummy agent-", "localhost", 12345, "http://localhost:12345/", "-dummy token-");
        entityManager.persist(agent);
        testResource.setAgent(agent);
        agentServiceContainer.addStartedAgent(agent);
        agentId = agent.getId();
    }

    /**
     * A dummy that needs to be set up before running ResourceManager.deleteResource()
     */
    public class MockAgentService implements MeasurementAgentService {

        public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, DataType dataType, String... measurementNames) {
            // TODO Auto-generated method stub
            return null;
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

    }

}