package org.rhq.enterprise.server.ws;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.utility.WsUtility;

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
public class WsSubjectTest extends AssertJUnit {

    private static final boolean TESTS_ENABLED = true;
    private static String credentials = "ws-test";
    private static String host = "127.0.0.1";
    private static int port = 7080;

    private static void init() {
        //TODO: write code to setup the ws-test user with correct roles
    }

    @BeforeTest
    public void setup() {
    }

    @AfterTest
    public void teardown() {
    }

    //Create and use SubjectManagerBean
    @Test(enabled = TESTS_ENABLED)
    public void testSubject() throws java.lang.Exception {

        //Build URL references to connect to running WS enabled server
        URL sUrl = WsUtility.generateRhqRemoteWebserviceURL(SubjectManagerBeanService.class, host, port, false);
        QName sQName = WsUtility.generateRhqRemoteWebserviceQName(SubjectManagerBeanService.class);
        SubjectManagerBeanService smService = new SubjectManagerBeanService(sUrl, sQName);
        SubjectManagerRemote subjectManager = smService.getSubjectManagerBeanPort();
        Subject user = subjectManager.login(credentials, credentials);

        //test ref
        assertNotNull(user);

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
        //        Subject newUser = subjectManager.findSubjectByName(user, testUserName);
        Subject newUser = subjectManager.getSubjectByName(testUserName);

        //if there, must have been from previous failed test... clean up.
        if (null != newUser) {
            //            Integer[] list = new Integer[newUser.getId()];
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(newUser.getId());

            subjectManager.deleteSubjects(user, list);
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
        newUser = subjectManager.getSubjectByName(testUserName);
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
        //        assertTrue(subjectManager.isLoggedIn(testUserName));
        subjectManager.logout(newUser);
        //verify logout
        //        assertFalse(subjectManager.isLoggedIn(testUserName));

        //test password change methods
        subjectManager.changePassword(user, testUserName, "updated-ws-test-password");
        newUser = subjectManager.login(testUserName, "updated-ws-test-password");

        //        assertTrue(subjectManager.isLoggedIn(testUserName));
        subjectManager.logout(newUser);
        //        assertFalse(subjectManager.isLoggedIn(testUserName));

        //locate subject
        newUser = subjectManager.getSubjectByName(testUserName);
        //more subject modification testing .. this time using update()
        newUser.setFirstName("updated-first-name");
        newUser.setLastName("updated-last-name");
        subjectManager.updateSubject(user, newUser);

        //checking for successful update
        newUser = subjectManager.getSubjectByName(testUserName);

        assertTrue("ws-test-user".equals(newUser.getName()));
        assertTrue("updated-first-name".equals(newUser.getFirstName()));
        assertTrue("updated-last-name".equals(newUser.getLastName()));

        //ROLE testing
        //make connection to Role Bean

        //        RoleManagerRemote roleManager = remoteClientRef.getRoleManagerRemote();
        URL rUrl = WsUtility.generateRhqRemoteWebserviceURL(RoleManagerBeanService.class, host, port, false);
        QName rQName = WsUtility.generateRhqRemoteWebserviceQName(RoleManagerBeanService.class);
        RoleManagerBeanService rmService = new RoleManagerBeanService(rUrl, rQName);
        RoleManagerRemote roleManager = rmService.getRoleManagerBeanPort();
        //        Subject user = subjectManager.login(credentials, credentials);

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
        //        roles = roleManager.getAvailableRolesForSubject(user, newUser.getId(), emptyList, pageControl);
        //        roles = roleManager.findSubjectAssignedRoles(user, newUser.getId(), pageControl);
        roles = roleManager.findSubjectAssignedRoles(user, user.getId(), pageControl);
        //               displayXml(roles);

        //locate 'All Role Id'
        int roleId = -1;
        for (Role role : roles) {
            //displayXml(role);
            if ("all resources role".equalsIgnoreCase(role.getName())) {
                roleId = role.getId();
                break;
            }
        }
        assertTrue("Role was not successfully located.", (roleId != -1));

        //assign that role to the subject
        //        Integer[] roleBag = new Integer[] { roleId };
        List<Integer> roleBag = new ArrayList<Integer>();
        roleBag.add(roleId);
        //        roleManager.assignRolesToSubject(user, Integer.valueOf(newUser.getId()), roleBag);
        roleManager.addRolesToSubject(user, Integer.valueOf(newUser.getId()), roleBag);

        //check that assignment occurred
        //        List<Subject> attachedSubjects = roleManager.getRoleSubjects(user, roleId, pageControl);
        //        List<Role> attachedSubjects = roleManager.findSubjectAssignedRoles(user, roleId, pageControl);
        List<Role> attachedSubjects = roleManager.findSubjectAssignedRoles(user, newUser.getId(), pageControl);
        boolean found = false;
        //        for (Subject subject : attachedSubjects) {
        for (Role subject : attachedSubjects) {

            if (subject.getId() == roleId) {
                found = true;
            }
        }
        assertTrue(found);

        //now cleanup the user and association we just created.
        List<Integer> cleanup = new ArrayList<Integer>();
        cleanup.add(newUser.getId());
        subjectManager.deleteSubjects(user, cleanup);
        assertNull(subjectManager.getSubjectByName("ws-test-user"));

    }

}
