package org.rhq.enterprise.server.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ChannelManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * These tests can not be executed in our standard unit test fashion as they require a running RHQ
 * Server with our web services deployed.
 * 
 * This is still in development and has the current restrictions:
 * - Server running on localhost.
 * - ws-test user defined in database with full permissions
 * - Non RHQ Server JBossAS in inventory.
 * - The jbossws-native-home env variable must be set to a jbossws native stack install (e.g. jbossws-native-3.0.4.GA.zip)
 * - The ws.test.package-path and ws.test.package-version environment variables must be defined to a test .war file.  
 * 
 * @author Jay Shaughnessy
 */

@Test(groups = "ws")
public class TestRemoteInterface extends AssertJUnit {

    static private final boolean TESTS_ENABLED = false;

    static private final String WSDL_URL_PREFIX = "http://127.0.0.1:7080/rhq-rhq-enterprise-server-ejb3/";
    static private final String TARGET_NS_CHANNEL_MANAGER = "http://content.server.enterprise.rhq.org/";
    static private final String TARGET_NS_CONFIGURATION_MANAGER = "http://configuration.server.enterprise.rhq.org/";
    static private final String TARGET_NS_CONTENT_MANAGER = "http://content.server.enterprise.rhq.org/";
    static private final String TARGET_NS_OPERATION_MANAGER = "http://operation.server.enterprise.rhq.org/";
    static private final String TARGET_NS_RESOURCE_MANAGER = "http://resource.server.enterprise.rhq.org/";
    static private final String TARGET_NS_ROLE_MANAGER = "http://authz.server.enterprise.rhq.org/";
    static private final String TARGET_NS_SUBJECT_MANAGER = "http://auth.server.enterprise.rhq.org/";

    private SubjectManagerRemote subjectManager;
    private Subject user;

    @BeforeMethod
    public void beforeMethod() throws Exception {
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        if (null != user) {
            subjectManager.logout(user);
        }
        user = null;
    }

    //@Test(enabled = TESTS_ENABLED)    
    @Test(enabled = true)
    public void testUserManagement() throws Exception {
        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        assertNotNull(user);

        assertEquals("ws-test", user.getName());

        assertNotNull(user.getSessionId());

        assertTrue(user.getSessionId() != 0);

        Subject newUser = subjectManager.getSubjectByName("ws-test-user");

        if (null != newUser) {
            subjectManager.deleteSubjects(user, new int[] { newUser.getId() });
        }

        newUser = new Subject();
        newUser.setName("ws-test-user");
        newUser.setFirstName("first-name");
        newUser.setLastName("last-name");
        newUser.setFactive(true);
        subjectManager.createSubject(user, newUser);

        newUser = subjectManager.getSubjectByName("ws-test-user");
        assertNotNull(newUser);
        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("first-name".equals(newUser.getFirstName()));
        assertTrue("last-name".equals(newUser.getLastName()));

        subjectManager.createPrincipal(user, "ws-test-user", "ws-test-password");
        newUser = subjectManager.login("ws-test-user", "ws-test-password");
        subjectManager.logout(newUser);

        subjectManager.changePassword(user, "ws-test-user", "updated-ws-test-password");
        newUser = subjectManager.login("ws-test-user", "updated-ws-test-password");
        subjectManager.logout(newUser);

        newUser = subjectManager.getSubjectByName("ws-test-user");
        newUser.setFirstName("updated-first-name");
        newUser.setLastName("updated-last-name");
        subjectManager.updateSubject(user, newUser);

        newUser = subjectManager.getSubjectByName("ws-test-user");
        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("updated-first-name".equals(newUser.getFirstName()));
        assertTrue("updated-last-name".equals(newUser.getLastName()));

        wsdlURL = new URL(WSDL_URL_PREFIX + "RoleManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_ROLE_MANAGER, "RoleManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        RoleManagerRemote roleManager = service.getPort(RoleManagerRemote.class);

        PageList<Role> roles = roleManager.findSubjectUnassignedRoles(user, newUser.getId(), PageControl
            .getUnlimitedInstance());
        Integer roleId = 0;
        for (Role role : roles) {
            if ("all resources role".equalsIgnoreCase(role.getName())) {
                roleId = role.getId();
                break;
            }
        }
        roleManager.addRolesToSubject(user, newUser.getId(), new int[] { roleId });

        subjectManager.deleteSubjects(user, new int[] { newUser.getId() });
        assertNull(subjectManager.getSubjectByName("ws-test-user"));
    }

    @Test(enabled = TESTS_ENABLED)
    public void testOperation() throws Exception {

        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        wsdlURL = new URL(WSDL_URL_PREFIX + "ResourceManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_RESOURCE_MANAGER, "ResourceManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ResourceManagerRemote resourceManager = service.getPort(ResourceManagerRemote.class);

        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, ResourceCategory.PLATFORM,
            null, 0, null, PageControl.getUnlimitedInstance());

        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        ResourceComposite testPlatform = resources.get(0);

        wsdlURL = new URL(WSDL_URL_PREFIX + "OperationManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_OPERATION_MANAGER, "OperationManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        OperationManagerRemote operationManager = service.getPort(OperationManagerRemote.class);

        // Remove any pending histories
        PageList<ResourceOperationHistory> history = operationManager.findPendingResourceOperationHistories(user,
            testPlatform.getResource().getId(), PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.findPendingResourceOperationHistories(user, testPlatform.getResource().getId(),
                PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // remove any completed histories
        history = operationManager.findCompletedResourceOperationHistories(user, testPlatform.getResource().getId(),
            null, null, PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.findCompletedResourceOperationHistories(user,
                testPlatform.getResource().getId(), null, null, PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // schedule an operation for process list fetch
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(user, testPlatform
            .getResource().getId(), "viewProcessList", 0L, 0L, 0, 0, null, "ws unit test - platform process list");
        assertNotNull(schedule);

        // wait to finish, or try to cancel after a minute.
        history = operationManager.findPendingResourceOperationHistories(user, testPlatform.getResource().getId(),
            PageControl.getUnlimitedInstance());
        assertTrue(history.isEmpty() || history.size() == 1);

        long now = System.currentTimeMillis();
        do {
            history = operationManager.findPendingResourceOperationHistories(user, testPlatform.getResource().getId(),
                PageControl.getUnlimitedInstance());
        } while (!history.isEmpty() && (System.currentTimeMillis() < (now + 60000L)));
        if (!history.isEmpty()) {
            operationManager.cancelOperationHistory(user, history.get(0).getId(), true);
        }

        history = operationManager.findCompletedResourceOperationHistories(user, testPlatform.getResource().getId(),
            null, null, PageControl.getUnlimitedInstance());
        assertEquals(1, history.size());

        // remove scheduled operations
        List<ResourceOperationSchedule> schedules = operationManager.findScheduledResourceOperations(user, testPlatform
            .getResource().getId());
        assertEquals(1, history.size());

        if (!schedules.isEmpty()) {
            for (ResourceOperationSchedule ros : schedules) {
                operationManager.unscheduleResourceOperation(user, ros.getJobId().toString(), testPlatform
                    .getResource().getId());
            }

            schedules = operationManager.findScheduledResourceOperations(user, testPlatform.getResource().getId());
            assertTrue(schedules.isEmpty());
        }

        // schedule an operation for delayed process list fetch (we won't let it complete)
        schedule = operationManager.scheduleResourceOperation(user, testPlatform.getResource().getId(),
            "viewProcessList", 300000L, 0L, 0, 0, null, "unit test - shutdown operation");

        schedules = operationManager.findScheduledResourceOperations(user, testPlatform.getResource().getId());
        assertEquals(1, schedules.size());
        assertEquals(schedule.getJobId(), schedules.get(0).getJobId());

        operationManager.unscheduleResourceOperation(user, schedules.get(0).getJobId().toString(), testPlatform
            .getResource().getId());
        schedules = operationManager.findScheduledResourceOperations(user, testPlatform.getResource().getId());
        assertTrue(schedules.isEmpty());
    }

    // disabled by default because this tests in not repeatable, it currently tests the ability to shutdown an AS.
    // Once it's down it's down and has to be started manually at the moment.
    // The testOperation test is similar but repeatable. This is here only because it maps directly to a use case
    @Test(enabled = false)
    public void testJBossAS() throws Exception {

        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        wsdlURL = new URL(WSDL_URL_PREFIX + "ResourceManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_RESOURCE_MANAGER, "ResourceManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ResourceManagerRemote resourceManager = service.getPort(ResourceManagerRemote.class);

        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "JBossAS Server", 0,
            null, PageControl.getUnlimitedInstance());

        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        ResourceComposite testAS = null;
        for (ResourceComposite resource : resources) {
            if (!resource.getResource().getName().contains("RHQ Server")) {
                testAS = resource;
                break;
            }
        }

        assertNotNull("Test requires a Non-RHQ AS Server, please start and import a JBoss AS", testAS);

        wsdlURL = new URL(WSDL_URL_PREFIX + "OperationManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_OPERATION_MANAGER, "OperationManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        OperationManagerRemote operationManager = service.getPort(OperationManagerRemote.class);

        // Remove any pending histories
        PageList<ResourceOperationHistory> history = operationManager.findPendingResourceOperationHistories(user,
            testAS.getResource().getId(), PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.findPendingResourceOperationHistories(user, testAS.getResource().getId(),
                PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // remove any completed histories
        history = operationManager.findCompletedResourceOperationHistories(user, testAS.getResource().getId(), null,
            null, PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.findCompletedResourceOperationHistories(user, testAS.getResource().getId(),
                null, null, PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // schedule an operation for immediate shutdown of AS instance
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(user, testAS.getResource()
            .getId(), "shutdown", 0L, 0L, 0, 0, null, "unit test - shutdown operation");

        // wait to finish, or try to cancel after a minute.
        history = operationManager.findPendingResourceOperationHistories(user, testAS.getResource().getId(),
            PageControl.getUnlimitedInstance());
        assertTrue(history.isEmpty() || history.size() == 1);

        long now = System.currentTimeMillis();
        do {
            history = operationManager.findPendingResourceOperationHistories(user, testAS.getResource().getId(),
                PageControl.getUnlimitedInstance());
        } while (!history.isEmpty() && (System.currentTimeMillis() < (now + 60000L)));
        if (!history.isEmpty()) {
            operationManager.cancelOperationHistory(user, history.get(0).getId(), true);
        }

        history = operationManager.findCompletedResourceOperationHistories(user, testAS.getResource().getId(), null,
            null, PageControl.getUnlimitedInstance());
        assertEquals(1, history.size());

        // remove scheduled operations
        List<ResourceOperationSchedule> schedules = operationManager.findScheduledResourceOperations(user, testAS
            .getResource().getId());
        assertEquals(1, history.size());

        if (!schedules.isEmpty()) {
            for (ResourceOperationSchedule ros : schedules) {
                operationManager.unscheduleResourceOperation(user, ros.getJobId().toString(), testAS.getResource()
                    .getId());
            }

            schedules = operationManager.findScheduledResourceOperations(user, testAS.getResource().getId());
            assertTrue(schedules.isEmpty());
        }

        // schedule an operation for delayed shutdown of AS instance (we won't let it complete)
        schedule = operationManager.scheduleResourceOperation(user, testAS.getResource().getId(), "shutdown", 300000L,
            0L, 0, 0, null, "unit test - shutdown operation");

        schedules = operationManager.findScheduledResourceOperations(user, testAS.getResource().getId());
        assertEquals(1, schedules.size());
        assertEquals(schedule.getJobId(), schedules.get(0).getJobId());

        operationManager.unscheduleResourceOperation(user, schedules.get(0).getJobId().toString(), testAS.getResource()
            .getId());
        schedules = operationManager.findScheduledResourceOperations(user, testAS.getResource().getId());
        assertTrue(schedules.isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdateResourceConfiguration() throws Exception {

        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        wsdlURL = new URL(WSDL_URL_PREFIX + "ResourceManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_RESOURCE_MANAGER, "ResourceManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ResourceManagerRemote resourceManager = service.getPort(ResourceManagerRemote.class);

        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "RHQ Agent", 0,
            null, PageControl.getUnlimitedInstance());

        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        ResourceComposite testAgent = null;
        for (ResourceComposite resource : resources) {
            if (resource.getAvailability().equals(AvailabilityType.UP)) {
                testAgent = resource;
                break;
            }
        }

        assertNotNull("Test requires an available RHQ Agent, please start an RHQ Agent", testAgent);

        wsdlURL = new URL(WSDL_URL_PREFIX + "ConfigurationManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_CONFIGURATION_MANAGER, "ConfigurationManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ConfigurationManagerRemote configManager = service.getPort(ConfigurationManagerRemote.class);

        Configuration config = configManager.getResourceConfiguration(user, testAgent.getResource().getId());
        assertNotNull(config);
        assertEquals("plugins", config.getSimpleProperties().get("rhq.agent.plugins.directory").getStringValue());

        config.getSimpleProperties().get("rhq.agent.plugins.directory").setStringValue("plugins/../plugins");
        configManager.updateResourceConfiguration(user, testAgent.getResource().getId(), config);

        long now = System.currentTimeMillis();
        do {
        } while (configManager.isResourceConfigurationUpdateInProgress(user, testAgent.getResource().getId())
            && (System.currentTimeMillis() < (now + 60000L)));
        if (configManager.isResourceConfigurationUpdateInProgress(user, testAgent.getResource().getId())) {
            fail("Config Update not completed, may need to fix property manually");
        }

        config = configManager.getResourceConfiguration(user, testAgent.getResource().getId());
        assertNotNull(config);
        assertEquals("plugins/../plugins", config.getSimpleProperties().get("rhq.agent.plugins.directory")
            .getStringValue());

        config.getSimpleProperties().get("rhq.agent.plugins.directory").setStringValue("plugins");
        configManager.updateResourceConfiguration(user, testAgent.getResource().getId(), config);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdatePluginConfiguration() throws Exception {

        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        wsdlURL = new URL(WSDL_URL_PREFIX + "ResourceManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_RESOURCE_MANAGER, "ResourceManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ResourceManagerRemote resourceManager = service.getPort(ResourceManagerRemote.class);

        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "JBossAS Server", 0,
            null, PageControl.getUnlimitedInstance());

        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        ResourceComposite testAS = null;
        for (ResourceComposite resource : resources) {
            if (resource.getResource().getName().contains("RHQ Server")) {
                testAS = resource;
                break;
            }
        }

        assertNotNull("Could not find RHQ Server, that's not good...", testAS);

        wsdlURL = new URL(WSDL_URL_PREFIX + "ConfigurationManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_CONFIGURATION_MANAGER, "ConfigurationManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ConfigurationManagerRemote configManager = service.getPort(ConfigurationManagerRemote.class);

        Configuration config = configManager.getPluginConfiguration(user, testAS.getResource().getId());
        assertNotNull(config);
        assertTrue(config.getSimpleProperties().get("startScript").getStringValue().endsWith("run.bat"));

        String newString = config.getSimpleProperties().get("startScript").getStringValue().replace("run.bat",
            "batter.run");
        config.getSimpleProperties().get("startScript").setStringValue(newString);
        configManager.updatePluginConfiguration(user, testAS.getResource().getId(), config);

        config = configManager.getPluginConfiguration(user, testAS.getResource().getId());
        assertNotNull(config);
        assertTrue(config.getSimpleProperties().get("startScript").getStringValue().endsWith("batter.run"));

        newString = config.getSimpleProperties().get("startScript").getStringValue().replace("batter.run", "run.bat");
        config.getSimpleProperties().get("startScript").setStringValue(newString);
        configManager.updatePluginConfiguration(user, testAS.getResource().getId(), config);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeployment() throws Exception {

        reportHeap("start");

        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        reportHeap("subjectManager");

        user = subjectManager.login("ws-test", "ws-test");

        reportHeap("login");

        wsdlURL = new URL(WSDL_URL_PREFIX + "ResourceManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_RESOURCE_MANAGER, "ResourceManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ResourceManagerRemote resourceManager = service.getPort(ResourceManagerRemote.class);

        reportHeap("resourceManager");

        //        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "JBossAS Server", 0,
        //            null, PageControl.getUnlimitedInstance());
        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null,
            "Web Application (WAR)", 0, null, PageControl.getUnlimitedInstance());

        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        ResourceComposite testWar = null;
        for (ResourceComposite resource : resources) {
            // if (!resource.getResource().getName().contains("RHQ Server")) {
            if (resource.getResource().getName().contains("servlets-examples1.war")) {
                testWar = resource;
                break;
            }
        }

        assertNotNull("Test requires a Non-RHQ AS Server, please start and import a JBoss AS", testWar);

        wsdlURL = new URL(WSDL_URL_PREFIX + "ChannelManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_CHANNEL_MANAGER, "ChannelManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ChannelManagerRemote channelManager = service.getPort(ChannelManagerRemote.class);

        reportHeap("channelManager");

        PageList<Channel> channels = channelManager.findChannels(user, PageControl.getUnlimitedInstance());

        for (Channel channel : channels) {
            if ("ws-test-channel".equals(channel.getName())) {
                channelManager.deleteChannel(user, channel.getId());
            }
        }

        Channel testChannel = channelManager.createChannel(user, new Channel("ws-test-channel"));
        assertNotNull(testChannel);
        assertEquals("ws-test-channel", testChannel.getName());

        channels = channelManager.findChannels(user, PageControl.getUnlimitedInstance());

        testChannel = null;
        for (Channel channel : channels) {
            if ("ws-test-channel".equals(channel.getName())) {
                testChannel = channel;
                break;
            }
        }
        assertNotNull(testChannel);

        channelManager.subscribeResourceToChannels(user, testWar.getResource().getId(),
            new int[] { testChannel.getId() });

        PageList<Resource> channelResources = channelManager.findSubscribedResources(user, testChannel.getId(),
            PageControl.getUnlimitedInstance());

        assertNotNull(channelResources);
        assertEquals(1, channelResources.size());
        assertTrue(channelResources.get(0).equals(testWar.getResource()));

        wsdlURL = new URL(WSDL_URL_PREFIX + "ContentManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_CONTENT_MANAGER, "ContentManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        ContentManagerRemote contentManager = service.getPort(ContentManagerRemote.class);

        reportHeap("contentManager");

        List<PackageType> packageTypes = contentManager.findPackageTypes(user, "Web Application (WAR)", "JBossAS");
        assertNotNull(packageTypes);
        assertEquals(1, packageTypes.size());
        PackageType testPackageType = null;
        for (PackageType pt : packageTypes) {
            if ("file".equals(pt.getName())) {
                testPackageType = pt;
                break;
            }
        }
        assertNotNull(testPackageType);

        List<Architecture> architectures = contentManager.findArchitectures(user);
        assertNotNull(architectures);
        Architecture testArch = null;
        for (Architecture arch : architectures) {
            if ("noarch".equalsIgnoreCase(arch.getName())) {
                testArch = arch;
                break;
            }
        }
        assertNotNull(testArch);

        String packagePath = System.getProperty("ws.test.package-path");
        assertNotNull(packagePath);
        packagePath = packagePath.trim();
        assertTrue(!"".equals(packagePath));
        File file = new File(packagePath);
        assertTrue(file.exists());
        String packageVersion = System.getProperty("ws.test.package-version");
        assertNotNull(packageVersion);
        packageVersion = packageVersion.trim();
        assertTrue(!"".equals(packageVersion));

        //        PackageVersion testPackageVersion = contentManager.createPackageVersion(user, "ws-test-package",
        //            testPackageType.getId(), "1.0", testArch.getId(), getBytesFromFile(file));
        PackageVersion testPackageVersion = contentManager.createPackageVersion(user, "servlets-examples1.war",
            testPackageType.getId(), packageVersion, testArch.getId(), getBytesFromFile(file));
        assertNotNull(testPackageVersion);
        assertTrue(testPackageVersion.getId() > 0);

        channelManager.addPackageVersionsToChannel(user, testChannel.getId(), new int[] { testPackageVersion.getId() });

        int[] resourceIds = new int[] { testWar.getResource().getId() };
        int[] packageVersionIds = new int[] { testPackageVersion.getId() };

        contentManager.deployPackages(user, resourceIds, packageVersionIds);

        channelManager.unsubscribeResourceFromChannels(user, testWar.getResource().getId(), new int[] { testChannel
            .getId() });

        channelResources = channelManager.findSubscribedResources(user, testChannel.getId(), PageControl
            .getUnlimitedInstance());

        assertNotNull(channelResources);
        assertEquals(0, channelResources.size());

        // this will force a purge of testPackageVersion
        channelManager.deleteChannel(user, testChannel.getId());
    }

    private void reportHeap(String description) {
        Runtime runtime = Runtime.getRuntime();
        long mbConst = 1024 * 1024L;

        System.out.println(description);

        // Get current size of heap in bytes
        System.out.print("  totalMb=" + ((runtime.totalMemory() / mbConst)));

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        System.out.print(", maxMb=" + ((runtime.maxMemory() / mbConst)));

        // Get amount of free memory within the heap in bytes. This size will increase
        // after garbage collection and decrease as new objects are created.
        System.out.println(", freeMb=" + ((runtime.freeMemory() / mbConst)));
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
}
