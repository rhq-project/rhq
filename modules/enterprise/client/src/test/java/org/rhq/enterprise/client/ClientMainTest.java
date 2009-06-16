package org.rhq.enterprise.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

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
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.client.utility.Utility;
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
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
//public class ClientMainTest extends AssertJUnit {
public class ClientMainTest extends TestCase {

    static private final boolean TESTS_ENABLED = true;

    //test constants
    private static String credentials = "ws-test";

    //TODO: figure out if there is a way to use just JAXB. One less dependency. Also will be more picky
    //private static XStream XS = new XStream();
    public static PageControl pagecontrol_unlimited = null;
    private static boolean isWstestUserCreated = false;
    private static String host = "127.0.0.1";
    private static int port = 7080;
    private static RHQRemoteClient remoteClientRef = null;

    static{
        if(pagecontrol_unlimited!=null){
            pagecontrol_unlimited = new PageControl();
            pagecontrol_unlimited.setPageSize(-1);
          }
          if(remoteClientRef==null){
              remoteClientRef = new RHQRemoteClient(host,port);
          }
    }

    @BeforeMethod
    protected void beforeMethod() throws Exception {
        if(pagecontrol_unlimited!=null){
          pagecontrol_unlimited = new PageControl();
          pagecontrol_unlimited.setPageSize(-1);
        }
        if(remoteClientRef==null){
            remoteClientRef = new RHQRemoteClient(host,port);
        }
//        //Checks for Ws-test user and creates it if it doesn't exist
//        if(!isWstestUserCreated){
//        	//assumes default rhqadmin details are in effect
////        	SubjectManagerBeanService smService = new SubjectManagerBeanService();
//        	URL sUrl = Utility.generateRhqRemoteWebserviceURL(SubjectManagerBeanService.class,
//        			host, port, false);
//        	QName sQName = Utility.generateRhqRemoteWebserviceQName(SubjectManagerBeanService.class);
//        	SubjectManagerBeanService smService = new SubjectManagerBeanService(sUrl,sQName);
//            SubjectManagerRemote subjectManager = smService.getSubjectManagerBeanPort();
//            Subject user = subjectManager.login("rhqadmin", "rhqadmin");
//            Subject wsTestUser = subjectManager.findSubjectByName(user, credentials);
//            if((wsTestUser!=null) &&(wsTestUser.getId()>0)){
//            	isWstestUserCreated = true;
//            }
//            //Create ws-test user
//        	if(!isWstestUserCreated){
//                Subject newUser = new Subject();
//                //        if(logXmlToScreen){displayXml(newUser);}
//
//                String fName = "WS";
//                String lName = "Test";
//                newUser.setName(credentials);
//                newUser.setFirstName(fName);
//                newUser.setLastName(lName);
//                newUser.setFactive(true);
//
//                //Send command to create the new user
//                subjectManager.createSubject(user, newUser);
//
//               //Now locate role and attach
//                //make connection to Role Bean
////                RoleManagerBeanService rmService = new RoleManagerBeanService();
//            	URL rUrl = Utility.generateRhqRemoteWebserviceURL(RoleManagerBeanService.class,
//            			host, port, false);
//            	QName rQName = Utility.generateRhqRemoteWebserviceQName(RoleManagerBeanService.class);
//                RoleManagerBeanService rmService = new RoleManagerBeanService(rUrl,rQName);
//                RoleManagerRemote roleManager = rmService.getRoleManagerBeanPort();
//                List<Integer> emptyList = null;
//                List<Role> roles = null;
//                roles = roleManager.getAvailableRolesForSubject(user, Integer.valueOf(newUser.getId()),
//                		emptyList, pagecontrol_unlimited);
//
//                //locate 'All Role Id'
//                int roleId = 0;
//                for (Role role : roles) {
//                    if ("all resources role".equalsIgnoreCase(role.getName())) {
//                        roleId = role.getId();
//                        break;
//                    }
//                }
//
//                //assign that role to the subject
//                List<Integer> roleBag = new ArrayList<Integer>();
//                roleBag.add(roleId);
//                roleManager.assignRolesToSubject(user, newUser.getId(), roleBag);
//
//        	  isWstestUserCreated = true;
//        	}
//        }

    }

    @AfterMethod
    protected void afterMethod() throws Exception {
    }

    //Create and use SubjectManagerBean
    @Test(enabled = TESTS_ENABLED)
    public void testSubject() throws Exception {

        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();
        Subject user = subjectManager.login(credentials, credentials);

        //test ref
        assertNotNull(user);

        //test isLoggedIn meth: Custom Method
        assertTrue(subjectManager.isLoggedIn(credentials));

        //test subject details: CREATE
        assertEquals(credentials, user.getName());

        //test session Id : CREATE
        assertNotNull(user.getSessionId());

        //test for invalid data in session id : CREATE
        assertTrue(user.getSessionId() != 0);

        //SUBJECT TESTING
        //check for user that shouldn't be there. Incomplete cleanups do occur and leave db debris
        String testUserName = "ws-test-user";
        //Use Case # 7. : SEARCH
        Subject newUser = subjectManager.findSubjectByName(user, testUserName);

        //if there, must have been from previous failed test... clean up.
        if (null != newUser) {
            Integer[] list = new Integer[newUser.getId()];
            subjectManager.deleteUsers(user, list);
        }

        //test adding a new user with details.
        newUser = new Subject();
        //        if(logXmlToScreen){displayXml(newUser);}

        String fName = "first-name";
        String lName = "last-name";
        newUser.setName(testUserName);
        newUser.setFirstName(fName);
        newUser.setLastName(lName);
        newUser.setFactive(true);
        //        displayXml(newUser);

        //Send command to create the new user
        subjectManager.createSubject(user, newUser);

        //locate the previously created user and test values
        newUser = subjectManager.findSubjectByName(user, testUserName);
        //        displayXml(newUser);

        assertNotNull(newUser);
        assertTrue(testUserName.equals(newUser.getName()));
        assertTrue(fName.equals(newUser.getFirstName()));
        assertTrue(lName.equals(newUser.getLastName()));

        //PRINCIPAL TESTING
        //create principal
        String testUserPass = "ws-test-password";

        //create principal
        subjectManager.createPrincipal(user, testUserName, testUserPass);
        //attempt login
        newUser = subjectManager.login(testUserName, testUserPass);

        //verify login
        assertTrue(subjectManager.isLoggedIn(testUserName));
        subjectManager.logout(newUser.getSessionId());
        //verify logout
        assertFalse(subjectManager.isLoggedIn(testUserName));

        //test password change methods
        subjectManager.changePassword(user, testUserName, "updated-ws-test-password");
        newUser = subjectManager.login(testUserName, "updated-ws-test-password");

        assertTrue(subjectManager.isLoggedIn(testUserName));
        subjectManager.logout(newUser.getSessionId());
        assertFalse(subjectManager.isLoggedIn(testUserName));

        //locate subject
        newUser = subjectManager.findSubjectByName(user, testUserName);
        //more subject modification testing .. this time using update()
        newUser.setFirstName("updated-first-name");
        newUser.setLastName("updated-last-name");
        subjectManager.updateSubject(user, newUser);

        //checking for successful update
        newUser = subjectManager.findSubjectByName(user, testUserName);

        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("updated-first-name".equals(newUser.getFirstName()));
        assertTrue("updated-last-name".equals(newUser.getLastName()));

        //ROLE testing
        //make connection to Role Bean

        RoleManagerRemote roleManager = remoteClientRef.getRoleManagerRemote();

        //locate roles available for subject
        //need to tell the bean how many roles to return for this enumeration
        //TODO:?? is there a smarter way to do this for remote clients? Maybe rec #? Smart Default?
        //        Expose additional method to page through using int value? Like the last one.
        //        Hmmm.. I'm thinking two methods
        //         i)getAvailableValuesWindow and returns range and chunk sizes available
        //         ii)Get values[in bookmark range] and response could be empty with first not being status of req.
        PageControl pageControl = new PageControl();
        pageControl.setPageSize(10);
        Integer[] emptyList = null;

        //Find roles possible to add. Ex. lay users cannot elevate another account to SYS ADM
        //  Access controls for Use Case #7
        List<Role> roles = null;
        roles = roleManager.getAvailableRolesForSubject(user, newUser.getId(), emptyList, pageControl);
        //               displayXml(roles);

        //locate 'All Role Id'
        int roleId = 0;
        for (Role role : roles) {
            //displayXml(role);
            if ("all resources role".equalsIgnoreCase(role.getName())) {
                roleId = role.getId();
                break;
            }
        }

        //assign that role to the subject
        Integer[] roleBag = new Integer[]{roleId};
        roleManager.assignRolesToSubject(user, Integer.valueOf(newUser.getId()), roleBag);

        //check that assignment occurred
        List<Subject> attachedSubjects = roleManager.getRoleSubjects(user, roleId, pageControl);
        boolean found = false;
        for (Subject subject : attachedSubjects) {
            if (subject.getId() == newUser.getId()) {
                found = true;
            }
        }
        assertTrue(found);

        //now cleanup the user and association we just created.
        Integer[] cleanup = new Integer[]{newUser.getId()};
        subjectManager.deleteUsers(user, cleanup);
        assertNull(subjectManager.findSubjectByName(user, "ws-test-user"));

    }

    @Test(enabled = TESTS_ENABLED)
    public void testOperation() throws Exception {

        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();
        Subject user = subjectManager.login(credentials, credentials);

        ResourceManagerRemote resourceManager = remoteClientRef.getResourceManagerRemote();

        PageControl pageControl = new PageControl();
        //displayXml(pageControl);
        pageControl.setPageSize(10);
        String s = null;
        List<ResourceComposite> resources = resourceManager.findResourceComposites(user, ResourceCategory.PLATFORM, s,
            0, s, pageControl);
        //displayXml(resources);
        assertNotNull(resources);
        assertTrue(resources.size() >= 1);

        //Should only be one platform
        ResourceComposite testPlatform = resources.get(0);
        //displayXml(testPlatform);

        OperationManagerRemote opManager = remoteClientRef.getOperationManagerRemote();

        List<ResourceOperationHistory> history = opManager.getPendingResourceOperationHistories(user, testPlatform
            .getResource().getId(), pageControl);
        displayXml(history);
        //check for previous dirty bits from last test and clean up
        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                opManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = opManager.getPendingResourceOperationHistories(user, testPlatform.getResource().getId(),
                pageControl);
            assertTrue(history.isEmpty());
        }

        System.out.println("PlatformOpId:" + testPlatform.getResource().getId());
        pageControl.setPageNumber(10);
        // remove any completed histories
        history = opManager.getCompletedResourceOperationHistories(user, testPlatform.getResource().getId(), null,
            null, pageControl);
        displayXml(history);
        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                opManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = opManager.getCompletedResourceOperationHistories(user, testPlatform.getResource().getId(), null,
                null, pageControl);
            assertTrue(history.isEmpty());
        }

        Configuration cf = new Configuration();
        ResourceOperationSchedule schedule = opManager.scheduleResourceOperation(user, testPlatform.getResource()
            .getId(), "viewProcessList", 0L, 0L, 0, 0, cf, "ws unit test - platform process list");
        assertNotNull(schedule);

        // wait to finish, or try to cancel after a minute.
        history = opManager.getPendingResourceOperationHistories(user, testPlatform.getResource().getId(), pageControl);
        assertTrue(history.isEmpty() || history.size() == 1);

        long now = System.currentTimeMillis();
        do {
            history = opManager.getPendingResourceOperationHistories(user, testPlatform.getResource().getId(),
                pageControl);
        } while (!history.isEmpty() && (System.currentTimeMillis() < (now + 60000L)));
        if (!history.isEmpty()) {
            opManager.cancelOperationHistory(user, history.get(0).getId(), true);
        }

        history = opManager.getCompletedResourceOperationHistories(user, testPlatform.getResource().getId(), null,
            null, pageControl);
        assertEquals(1, history.size());

        // remove scheduled operations
        List<ResourceOperationSchedule> schedules = opManager.getScheduledResourceOperations(user, testPlatform
            .getResource().getId());
        assertEquals(1, history.size());

        if (!schedules.isEmpty()) {
            for (ResourceOperationSchedule ros : schedules) {
                //??                opManager.unscheduleResourceOperation(user, ros.getJobId(), testPlatform
                opManager.unscheduleResourceOperation(user, ros.getJobName(), testPlatform.getResource().getId());
            }

            schedules = opManager.getScheduledResourceOperations(user, testPlatform.getResource().getId());
            assertTrue(schedules.isEmpty());
        }

        // schedule an operation for delayed process list fetch (we won't let it complete)
        schedule = opManager.scheduleResourceOperation(user, testPlatform.getResource().getId(), "viewProcessList",
            300000L, 0L, 0, 0, null, "unit test - shutdown operation");

        schedules = opManager.getScheduledResourceOperations(user, testPlatform.getResource().getId());
        assertEquals(1, schedules.size());
        //??        assertEquals(schedule.getJobId(), schedules.get(0).getJobId());
        assertEquals(schedule.getJobName(), schedules.get(0).getJobName());

        //        opManager.unscheduleResourceOperation(user, schedules.get(0).getJobId().toString(), testPlatform
        opManager.unscheduleResourceOperation(user, schedules.get(0).getJobName().toString(), testPlatform
            .getResource().getId());
        schedules = opManager.getScheduledResourceOperations(user, testPlatform.getResource().getId());
        assertTrue(schedules.isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testJBossAS() throws Exception {

        //instantiate SLSB
        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();
        Subject user = subjectManager.login("ws-test", "ws-test");

        //RemoteManagerBean
        ResourceManagerRemote resourceManager = remoteClientRef.getResourceManagerRemote();

        ResourceType rt = new ResourceType();
        ResourceSubCategory subCat = new ResourceSubCategory();
           subCat.setDescription("JBoss Application Server");
        rt.setSubCategory(subCat);

        //        PageList<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "JBossAS Server", 0,
        List<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "JBossAS Server", 0,
            null, pagecontrol_unlimited);
        //        null, PageControl.getUnlimitedInstance());

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

        OperationManagerRemote operationManager = remoteClientRef.getOperationManagerRemote();

        // Remove any pending histories
        List<ResourceOperationHistory> history = operationManager.getPendingResourceOperationHistories(user, testAS
            .getResource().getId(), pagecontrol_unlimited);

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(),
            //                PageControl.getUnlimitedInstance());
                pagecontrol_unlimited);
            assertTrue(history.isEmpty());
        }

        // remove any completed histories
        history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(), null,
            null, pagecontrol_unlimited);
        //        null, PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(), null,
            //                null, PageControl.getUnlimitedInstance());
                null, pagecontrol_unlimited);
            assertTrue(history.isEmpty());
        }

        // schedule an operation for immediate shutdown of AS instance
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(user, testAS.getResource()
            .getId(), "shutdown", 0L, 0L, 0, 0, null, "unit test - shutdown operation");

        // wait to finish, or try to cancel after a minute.
        history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(),
            pagecontrol_unlimited);
        //        PageControl.getUnlimitedInstance());
        assertTrue(history.isEmpty() || history.size() == 1);

        long now = System.currentTimeMillis();
        do {
            history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(),
                pagecontrol_unlimited);
            //            PageControl.getUnlimitedInstance());
        } while (!history.isEmpty() && (System.currentTimeMillis() < (now + 60000L)));
        if (!history.isEmpty()) {
            operationManager.cancelOperationHistory(user, history.get(0).getId(), true);
        }

        history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(), null,
            null, pagecontrol_unlimited);
        //        null, PageControl.getUnlimitedInstance());
        assertEquals(1, history.size());

        // remove scheduled operations
        List<ResourceOperationSchedule> schedules = operationManager.getScheduledResourceOperations(user, testAS
            .getResource().getId());
        assertEquals(1, history.size());

        if (!schedules.isEmpty()) {
            for (ResourceOperationSchedule ros : schedules) {
                //                operationManager.unscheduleResourceOperation(user, ros.getJobId().toString(), testAS.getResource()
                operationManager.unscheduleResourceOperation(user, ros.getJobName(), testAS.getResource().getId());
            }

            schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
            assertTrue(schedules.isEmpty());
        }

        // schedule an operation for delayed shutdown of AS instance (we won't let it complete)
        schedule = operationManager.scheduleResourceOperation(user, testAS.getResource().getId(), "shutdown", 300000L,
            0L, 0, 0, null, "unit test - shutdown operation");

        schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
        assertEquals(1, schedules.size());
        //        assertEquals(schedule.getJobId(), schedules.get(0).getJobId());
        assertEquals(schedule.getJobName(), schedules.get(0).getJobName());

        //        operationManager.unscheduleResourceOperation(user, schedules.get(0).getJobId().toString(), testAS.getResource()
        operationManager.unscheduleResourceOperation(user, schedules.get(0).getJobName(), testAS.getResource().getId());
        schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
        assertTrue(schedules.isEmpty());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdateResourceConfiguration() throws Exception {

        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();

        Subject user = subjectManager.login("ws-test", "ws-test");

        ResourceManagerRemote resourceManager = remoteClientRef.getResourceManagerRemote();

        List<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "RHQ Agent", 0, null,
            pagecontrol_unlimited);

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

        //instantiate SLSB
        ConfigurationManagerRemote configManager = remoteClientRef.getConfigurationManagerRemote();

        Configuration config = configManager.getCurrentResourceConfiguration(user, testAgent.getResource().getId());
        assertNotNull(config);
        //TODO: the following is NOT yet converted.  Not immediately apparent what needs to be changed.
        //        assertEquals("plugins", config.getSimpleProperties().get("rhq.agent.plugins.directory").getStringValue());
        //        assertEquals("plugins", config.getSimpleProperties().get("rhq.agent.plugins.directory").getStringValue());
        //
        //        config.getSimpleProperties().get("rhq.agent.plugins.directory").setStringValue("plugins/../plugins");
        //        configManager.updateResourceConfiguration(user, testAgent.getResource().getId(), config);
        //
        //        long now = System.currentTimeMillis();
        //        do {
        //        } while (configManager.isResourceConfigurationUpdateInProgress(user, testAgent.getResource().getId())
        //            && (System.currentTimeMillis() < (now + 60000L)));
        //        if (configManager.isResourceConfigurationUpdateInProgress(user, testAgent.getResource().getId())) {
        //            fail("Config Update not completed, may need to fix property manually");
        //        }
        //
        //        config = configManager.getCurrentResourceConfiguration(user, testAgent.getResource().getId());
        //        assertNotNull(config);
        //        assertEquals("plugins/../plugins", config.getSimpleProperties().get("rhq.agent.plugins.directory")
        //            .getStringValue());
        //
        //        config.getSimpleProperties().get("rhq.agent.plugins.directory").setStringValue("plugins");
        //        configManager.updateResourceConfiguration(user, testAgent.getResource().getId(), config);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUpdatePluginConfiguration() throws Exception {

        //instantiate SLSB
        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();
        Subject user = subjectManager.login("ws-test", "ws-test");

        //instantiate SLSB
        ResourceManagerRemote rmManager = remoteClientRef.getResourceManagerRemote();

        List<ResourceComposite> resources = rmManager.findResourceComposites(user, null, "JBossAS Server", 0, null,
            pagecontrol_unlimited);

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

        ConfigurationManagerRemote configManager = remoteClientRef.getConfigurationManagerRemote();

        Configuration config = configManager.getCurrentPluginConfiguration(user, testAS.getResource().getId());
        assertNotNull(config);
        //TODO: Fix below references. not immediately apparent what needs to be changed to.
        //        assertTrue(config.getSimpleProperties().get("startScript").getStringValue().endsWith("run.bat"));
        //
        //        String newString = config.getSimpleProperties().get("startScript").getStringValue().replace("run.bat",
        //            "batter.run");
        //        config.getSimpleProperties().get("startScript").setStringValue(newString);
        //        configManager.updatePluginConfiguration(user, testAS.getResource().getId(), config);
        //
        //        config = configManager.getCurrentPluginConfiguration(user, testAS.getResource().getId());
        //        assertNotNull(config);
        //        assertTrue(config.getSimpleProperties().get("startScript").getStringValue().endsWith("batter.run"));
        //
        //        newString = config.getSimpleProperties().get("startScript").getStringValue().replace("batter.run", "run.bat");
        //        config.getSimpleProperties().get("startScript").setStringValue(newString);
        //        configManager.updatePluginConfiguration(user, testAS.getResource().getId(), config);
    }

//    public void testDeployment() throws Exception, ResourceTypeNotFoundException {
    @Test(enabled = TESTS_ENABLED)
    public void testDeployment() throws Exception {

        reportHeap("start");

        SubjectManagerRemote subjectManager = remoteClientRef.getSubjectManagerRemote();

        reportHeap("subjectManager");

        Subject user = subjectManager.login("ws-test", "ws-test");

        reportHeap("login");

        ResourceManagerRemote resourceManager = remoteClientRef.getResourceManagerRemote();

        reportHeap("resourceManager");

        List<ResourceComposite> resources = resourceManager.findResourceComposites(user, null, "Web Application (WAR)",
            0, null, pagecontrol_unlimited);

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

        ChannelManagerRemote channelManager = remoteClientRef.getChannelManagerRemote();

        reportHeap("channelManager");

        List<Channel> channels = channelManager.getAllChannels(user, pagecontrol_unlimited);

        for (Channel channel : channels) {
            if ("ws-test-channel".equals(channel.getName())) {
                channelManager.deleteChannel(user, channel.getId());
            }
        }

        Channel ch = new Channel();
        ch.setName("ws-test-channel");
        Channel testChannel = channelManager.createChannel(user, ch);
        assertNotNull(testChannel);
        assertEquals("ws-test-channel", testChannel.getName());

        channels = channelManager.getAllChannels(user, pagecontrol_unlimited);

        testChannel = null;
        for (Channel channel : channels) {
            if ("ws-test-channel".equals(channel.getName())) {
                testChannel = channel;
                break;
            }
        }
        assertNotNull(testChannel);

        int[] bag = new int[]{testChannel.getId()};
        channelManager.subscribeResourceToChannels(user, testWar.getResource().getId(), bag);

        List<Resource> channelResources = channelManager.getSubscribedResources(user, testChannel.getId(),
            pagecontrol_unlimited);

        assertNotNull(channelResources);
        assertEquals(1, channelResources.size());
        assertTrue(channelResources.get(0).equals(testWar.getResource()));

        ContentManagerRemote contentManager = remoteClientRef.getContentManagerRemote();

        reportHeap("contentManager");

        List<PackageType> packageTypes = contentManager.getPackageTypes(user, "Web Application (WAR)", "JBossAS");
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

        List<Architecture> architectures = contentManager.getArchitectures(user);
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

        PackageVersion testPackageVersion = contentManager.createPackageVersion(user, "servlets-examples1.war",
            testPackageType.getId(), packageVersion, testArch.getId(), getBytesFromFile(file));
        assertNotNull(testPackageVersion);
        assertTrue(testPackageVersion.getId() > 0);

        int[] bag2 = new int[]{testPackageVersion.getId()};
        channelManager.addPackageVersionsToChannel(user, testChannel.getId(), bag2);

        Set<Integer> resourceSet = new HashSet<Integer>(1);
        Set<Integer> packageVersionSet = new HashSet<Integer>(1);

        resourceSet.add(testWar.getResource().getId());
        packageVersionSet.add(testPackageVersion.getId());

        contentManager.deployPackages(user, resourceSet, packageVersionSet);

        int[] chBag = new int[]{testChannel.getId()};
        channelManager.unsubscribeResourceFromChannels(user, testWar.getResource().getId(), chBag);

        channelResources = channelManager.getSubscribedResources(user, testChannel.getId(), pagecontrol_unlimited);

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

    private void displayXml(Object... elements) {
        for (Object element : elements) {
            //String xml = XS.toXML(element);
            String xml = element.toString();

            System.out.println("------- NOT JAXB XML: (XStream) ----:" + Object.class.getCanonicalName());
            System.out.println(xml + "\n\n");
        }
    }

}
