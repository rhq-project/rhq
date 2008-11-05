package org.rhq.enterprise.server.test;

import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
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
 * 
 * @author jay shaughnessy
 */

@Test
public class TestRemoteInterface extends AbstractEJB3Test {

    static private final boolean TESTS_ENABLED = false;

    static private final String WSDL_URL_PREFIX = "http://127.0.0.1:7080/rhq-rhq-enterprise-server-ejb3/";
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
            subjectManager.logout(user.getSessionId());
        }
        user = null;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testUserManagement() throws Exception {
        URL wsdlURL = new URL(WSDL_URL_PREFIX + "SubjectManagerBean?wsdl");
        QName serviceName = new QName(TARGET_NS_SUBJECT_MANAGER, "SubjectManagerBeanService");
        Service service = Service.create(wsdlURL, serviceName);
        subjectManager = service.getPort(SubjectManagerRemote.class);

        user = subjectManager.login("ws-test", "ws-test");

        assertTrue(subjectManager.isLoggedIn("ws-test"));

        assertEquals("ws-test", user.getName());

        assertNotNull(user.getSessionId());

        assertTrue(user.getSessionId() != 0);

        assertTrue(subjectManager.isLoggedIn("ws-test"));

        Subject newUser = subjectManager.findSubjectByName(user, "ws-test-user");

        if (null != newUser) {
            subjectManager.deleteUsers(user, new Integer[] { newUser.getId() });
        }

        newUser = new Subject();
        newUser.setName("ws-test-user");
        newUser.setFirstName("first-name");
        newUser.setLastName("last-name");
        newUser.setFactive(true);
        subjectManager.createSubject(user, newUser);

        newUser = subjectManager.findSubjectByName(user, "ws-test-user");
        assertNotNull(newUser);
        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("first-name".equals(newUser.getFirstName()));
        assertTrue("last-name".equals(newUser.getLastName()));

        subjectManager.createPrincipal(user, "ws-test-user", "ws-test-password");
        newUser = subjectManager.login("ws-test-user", "ws-test-password");
        assertTrue(subjectManager.isLoggedIn("ws-test-user"));
        subjectManager.logout(newUser.getSessionId());
        assertFalse(subjectManager.isLoggedIn("ws-test-user"));

        subjectManager.changePassword(user, "ws-test-user", "updated-ws-test-password");
        newUser = subjectManager.login("ws-test-user", "updated-ws-test-password");
        assertTrue(subjectManager.isLoggedIn("ws-test-user"));
        subjectManager.logout(newUser.getSessionId());
        assertFalse(subjectManager.isLoggedIn("ws-test-user"));

        newUser = subjectManager.findSubjectByName(user, "ws-test-user");
        newUser.setFirstName("updated-first-name");
        newUser.setLastName("updated-last-name");
        subjectManager.updateSubject(user, newUser);

        newUser = subjectManager.findSubjectByName(user, "ws-test-user");
        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("updated-first-name".equals(newUser.getFirstName()));
        assertTrue("updated-last-name".equals(newUser.getLastName()));

        wsdlURL = new URL(WSDL_URL_PREFIX + "RoleManagerBean?wsdl");
        serviceName = new QName(TARGET_NS_ROLE_MANAGER, "RoleManagerBeanService");
        service = Service.create(wsdlURL, serviceName);
        RoleManagerRemote roleManager = service.getPort(RoleManagerRemote.class);

        PageList<Role> roles = roleManager.getAvailableRolesForSubject(user, newUser.getId(), null, PageControl
            .getUnlimitedInstance());
        Integer roleId = 0;
        for (Role role : roles) {
            if ("all resources role".equalsIgnoreCase(role.getName())) {
                roleId = role.getId();
                break;
            }
        }
        roleManager.assignRolesToSubject(user, newUser.getId(), new Integer[] { roleId });

        subjectManager.deleteUsers(user, new Integer[] { newUser.getId() });
        assertNull(subjectManager.findSubjectByName(user, "ws-test-user"));
    }

    @Test(enabled = TESTS_ENABLED)
    public void testFindJBossAS() throws Exception {

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
        PageList<ResourceOperationHistory> history = operationManager.getPendingResourceOperationHistories(user, testAS
            .getResource().getId(), PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(),
                PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // remove any completed histories
        history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(),
            PageControl.getUnlimitedInstance());

        if (!history.isEmpty()) {
            for (ResourceOperationHistory roh : history) {
                operationManager.deleteOperationHistory(user, roh.getId(), true);
            }

            history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(),
                PageControl.getUnlimitedInstance());
            assertTrue(history.isEmpty());
        }

        // schedule an operation for immediate shutdown of AS instance
        ResourceOperationSchedule schedule = operationManager.scheduleResourceOperation(user, testAS.getResource()
            .getId(), "shutdown", 0L, 0L, 0, 0, null, "unit test - shutdown operation");

        // wait to finish, or try to cancel after a minute.
        history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(), PageControl
            .getUnlimitedInstance());
        assertTrue(history.isEmpty() || history.size() == 1);

        long now = System.currentTimeMillis();
        do {
            history = operationManager.getPendingResourceOperationHistories(user, testAS.getResource().getId(),
                PageControl.getUnlimitedInstance());
        } while (!history.isEmpty() && (System.currentTimeMillis() < (now + 60000L)));
        if (!history.isEmpty()) {
            operationManager.cancelOperationHistory(user, history.get(0).getId(), true);
        }

        history = operationManager.getCompletedResourceOperationHistories(user, testAS.getResource().getId(),
            PageControl.getUnlimitedInstance());
        assertEquals(1, history.size());

        // remove scheduled operations
        List<ResourceOperationSchedule> schedules = operationManager.getScheduledResourceOperations(user, testAS
            .getResource().getId());
        assertEquals(1, history.size());

        if (!schedules.isEmpty()) {
            for (ResourceOperationSchedule ros : schedules) {
                operationManager.unscheduleResourceOperation(user, ros.getJobId().toString(), testAS.getResource()
                    .getId());
            }

            schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
            assertTrue(schedules.isEmpty());
        }

        // schedule an operation for delayed shutdown of AS instance (we won't let it complete)
        schedule = operationManager.scheduleResourceOperation(user, testAS.getResource().getId(), "shutdown", 300000L,
            0L, 0, 0, null, "unit test - shutdown operation");

        schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
        assertEquals(1, schedules.size());
        assertEquals(schedule.getJobId(), schedules.get(0).getJobId());

        operationManager.unscheduleResourceOperation(user, schedules.get(0).getJobId().toString(), testAS.getResource()
            .getId());
        schedules = operationManager.getScheduledResourceOperations(user, testAS.getResource().getId());
        assertTrue(schedules.isEmpty());
    }
}
