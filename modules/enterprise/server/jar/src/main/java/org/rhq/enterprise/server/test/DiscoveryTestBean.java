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
package org.rhq.enterprise.server.test;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import org.rhq.core.clientapi.server.core.AgentNotSupportedException;
import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.CoreServerServiceImpl;
import org.rhq.enterprise.server.discovery.DiscoveryServerServiceImpl;

/**
 * An EJB for testing the discovery subsystem - used by TestControl.jsp.
 *
 * @author Ian Springer
 */
@Stateless
public class DiscoveryTestBean implements DiscoveryTestLocal {
    private final Log log = LogFactory.getLog(DiscoveryTestBean.class);

    private static final String TEST_PLUGIN_NAME = "BogusPlugin";
    private static final String TEST_PLUGIN_PATH = "on-bogus-plugin.jar";
    private static final String TEST_PLATFORM_TYPE_NAME = "BogusOS";
    private static final String TEST_PLATFORM_NAME = "BogusPlatform";
    private static final String TEST_SERVER_TYPE_NAME = "BogusApp";
    private static final String TEST_SERVER1_NAME = "BogusServer1";
    private static final String TEST_SERVER2_NAME = "BogusServer2";
    private static final String TEST_SERVICE_TYPE_NAME = "BogusServiceType";
    private static final String TEST_SERVICE1_NAME = "BogusService1";
    private static final String TEST_SERVICE2_NAME = "BogusService2";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private DiscoveryServerService discoveryServerService = new DiscoveryServerServiceImpl();

    private CoreServerService coreServerService = new CoreServerServiceImpl();

    @EJB
    private CoreTestLocal coreTest;

    @EJB
    private SubjectManagerLocal subjectManager;

    public void registerTestPluginAndTypeInfo() {
        //System.out.println("CALLER: " + ctx.getCallerPrincipal() +  " (" + ctx.getCallerPrincipal().getName() + ")");
        try {
            getTestPlugin();
        } catch (NoResultException e) {
            Plugin testPlugin = createTestPlugin();
            entityManager.persist(testPlugin);
            log.info("Test plugin [" + testPlugin + "] persisted...");
        }

        try {
            // This will also create a server and a service under the platform.
            getTestPlatformType();
        } catch (NoResultException e) {
            ResourceType testResourceType = createTestPlatformType();
            entityManager.persist(testResourceType);
            log.info("Test platform type [" + testResourceType + "] persisted...");
        }
    }

    public void removeTestPluginAndTypeInfo() {
        try {
            Plugin testPlugin = getTestPlugin();
            entityManager.remove(testPlugin);
            log.info("Test plugin [" + testPlugin + "] removed...");
        } catch (NoResultException e) {
            // ignore
        }

        try {
            ResourceType testPlatformType = getTestPlatformType();

            // NOTE: In addition to child server/service types, cascading will also remove all platform instances.
            entityManager.remove(testPlatformType);
            log.info("Test platform type [" + testPlatformType + "] recursively removed...");
        } catch (NoResultException e) {
            // ignore
        }
    }

    public void registerFakePlugin() {
        //ProductPluginManager ppm;
    }

    /**
     */
    public void sendTestFullInventoryReport() {
        log.info("Sending fake full inventory report to server...");
        Resource platform = createTestPlatform("127.0.0.2", 5, 10);
        try {
            InventoryReport report = new InventoryReport(this.coreTest.getTestAgent());
            report.addAddedRoot(platform);
            this.discoveryServerService.mergeInventoryReport(report);
        } catch (InvalidInventoryReportException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendNewPlatform(String platformAddress, int servers, int servicesPerServer) {
        AgentRegistrationRequest registrationRequest = new AgentRegistrationRequest("TestAgent", platformAddress, 2144,
            "socket://" + platformAddress + ":" + 2144, true, null, null);
        try {
            @SuppressWarnings("unused")
            AgentRegistrationResults results = this.coreServerService.registerAgent(registrationRequest);
        } catch (AgentRegistrationException e1) {
            throw new RuntimeException(e1);
        } catch (AgentNotSupportedException e2) {
            throw new RuntimeException(e2);
        }

        Resource platform = createTestPlatform(platformAddress, servers, servicesPerServer);
        try {
            InventoryReport report = new InventoryReport(new Agent("TestAgent", platformAddress, 2144, "endpoint",
                "token"));
            report.addAddedRoot(platform);
            this.discoveryServerService.mergeInventoryReport(report);
        } catch (InvalidInventoryReportException e) {
            throw new RuntimeException(e);
        }
    }

    public String clearAutoinventoryQueue() {
        return null;
    }

    public void sendTestRuntimeInventoryReport() {
        log.info("Sending fake runtime inventory report to server...");
        Resource platform = createTestPlatform(TEST_PLATFORM_NAME, 2, 10);
        addTestServicesToPlatform(platform);
        try {
            InventoryReport report = new InventoryReport(this.coreTest.getTestAgent());
            report.addAddedRoot(platform);
            this.discoveryServerService.mergeInventoryReport(report);
        } catch (InvalidInventoryReportException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeTestPlatform() {
        try {
            Resource platform = getTestPlatform();
            entityManager.remove(platform); // cascading will also remove test IPs and servers/services
        } catch (NoResultException e) {
            // ignore
        }
    }

    /*private void checkUserCanManageAgent(LatherContext ctx, String user,
     *                               String pword, String operation) throws PermissionException { // TODO: Implement
     * check (gh: what is with all the crazy caching?)
     */
    /*sessionId = this.getAuthManager().getSessionId(user, pword);
     * subject   =
     * this.sessionManager.getSubject(sessionId);this.getServerManager().checkCreatePlatformPermission(subject);*/
    /*
     *}*/

    public void createTestMixedGroup(String groupName) {
        List<Resource> resources = PersistenceUtility
            .findByCriteria(entityManager, Resource.class, Restrictions.like("name", "BogusServer 0",
                MatchMode.ANYWHERE), Restrictions.eq("inventoryStatus", InventoryStatus.COMMITTED));

        System.out.println("FOUND " + resources.size() + " resources");

        ResourceGroup group = new ResourceGroup(groupName);
        for (Resource resource : resources) {
            group.addExplicitResource(resource);
        }

        entityManager.persist(group);

        Subject s = new Subject(groupName + "User", true, false);
        entityManager.persist(s);

        Role r = new Role(groupName = "Role");
        r.addSubject(s);
        r.addResourceGroup(group);
        entityManager.persist(r);
    }

    private Resource getTestPlatform() {
        return (Resource) entityManager.createQuery("SELECT res FROM Resource res WHERE res.name = :name")
            .setParameter("name", TEST_PLATFORM_NAME).getSingleResult();
    }

    private Plugin getTestPlugin() {
        return (Plugin) entityManager.createNamedQuery(Plugin.QUERY_FIND_BY_NAME)
            .setParameter("name", TEST_PLUGIN_NAME).getSingleResult();
    }

    private ResourceType getTestPlatformType() {
        return (ResourceType) entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME).setParameter("name",
            TEST_PLATFORM_TYPE_NAME).getSingleResult();
    }

    private ResourceType getTestServerType() {
        return (ResourceType) entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME).setParameter("name",
            TEST_SERVER_TYPE_NAME).getSingleResult();
    }

    private ResourceType getTestServiceType() {
        return (ResourceType) entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME).setParameter("name",
            TEST_SERVICE_TYPE_NAME).getSingleResult();
    }

    private Resource createTestPlatform(String address, int servers, int servicesPerServer) {
        Resource platform = new Resource(address, address + " platform", getTestPlatformType());
        platform.setAgent(this.coreTest.getTestAgent());
        platform.setDescription("test platform created by " + this.getClass().getName());
        platform.setLocation("San Pedro Sula, Honduras");
        platform.setModifiedBy(subjectManager.getOverlord());

        //platform.addIP(createTestIp());
        String serverName = "BogusServer ";
        for (int i = 0; i < servers; i++) {
            platform.addChildResource(createTestServer(platform, serverName + i, servicesPerServer));
        }

        return platform;
    }

    private Resource createTestServer(Resource platform, String name, int servicesPerServer) {
        String installPath = "c:\\Program Files\\" + name;
        Resource server = new Resource(installPath, name, getTestServerType());
        server.setParentResource(platform);
        server.setDescription("test server created by " + this.getClass().getName());
        server.setModifiedBy(subjectManager.getOverlord());

        for (int i = 0; i < servicesPerServer; i++) {
            String childName = name + " child service " + i;
            server.addChildResource(createTestService(server, childName));
        }

        return server;
    }

    private Resource createTestService(Resource parent, String name) {
        Resource service = new Resource(name, name, getTestServiceType());
        service.setDescription("test service created by " + this.getClass().getName());
        service.setModifiedBy(subjectManager.getOverlord());
        return service;
    }

    private void addTestServicesToPlatform(Resource platform) {
        Resource testServer1 = platform.getChildResources().iterator().next();
        testServer1.addChildResource(createTestService(testServer1, TEST_SERVICE1_NAME));
        testServer1.addChildResource(createTestService(testServer1, TEST_SERVICE2_NAME));
    }

    private Plugin createTestPlugin() {
        return new Plugin(TEST_PLUGIN_NAME, TEST_PLUGIN_PATH, "abc123ddd76a2361be08b2b4c7f2b19b");
    }

    private ResourceType createTestPlatformType() {
        ResourceType platformType = new ResourceType(TEST_PLATFORM_TYPE_NAME, "", ResourceCategory.PLATFORM,
            ResourceType.ANY_PLATFORM_TYPE);
        platformType.setPlugin(TEST_PLUGIN_NAME);
        platformType.setDescription("test platform type");
        platformType.addChildResourceType(createTestServerType(platformType));
        return platformType;
    }

    private ResourceType createTestServerType(ResourceType parentType) {
        ResourceType serverType = new ResourceType(TEST_SERVER_TYPE_NAME, "", ResourceCategory.SERVER, parentType);
        serverType.setPlugin(TEST_PLUGIN_NAME);
        serverType.setDescription("test server type");
        serverType.addChildResourceType(createTestServiceType(serverType));
        return serverType;
    }

    private ResourceType createTestServiceType(ResourceType parentType) {
        ResourceType serviceType = new ResourceType(TEST_SERVICE_TYPE_NAME, "", ResourceCategory.SERVICE, parentType);
        serviceType.setPlugin(TEST_PLUGIN_NAME);
        serviceType.setDescription("test service type");
        return serviceType;
    }
}