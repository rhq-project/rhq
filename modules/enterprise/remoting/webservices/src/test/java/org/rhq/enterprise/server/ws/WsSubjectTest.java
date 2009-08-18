//package org.rhq.enterprise.server.ws;
//
//import java.lang.reflect.InvocationTargetException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.xml.namespace.QName;
//
//import org.testng.AssertJUnit;
//import org.testng.annotations.AfterTest;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.BeforeTest;
//import org.testng.annotations.Test;
//
//import org.rhq.enterprise.server.ws.utility.WsUtility;
//
///**
// * These tests can not be executed in our standard unit test fashion as they
// * require a running RHQ Server with our web services deployed.
// * 
// * This is still in development and has the current restrictions: - Server
// * running on localhost. - ws-test user defined in database with full
// * permissions - Non RHQ Server JBossAS in inventory. The ws.test.package-path and
// * ws.test.package-version environment variables must be defined to a test .war
// * file.
// * 
// * @author Jay Shaughnessy, Simeon Pinder
// */
//@Test(groups = "ws")
//public class WsSubjectTest extends AssertJUnit {
//
//    //Test variables
//    private static final boolean TESTS_ENABLED = true;
//    protected static String credentials = "ws-test";
//    protected static String host = "127.0.0.1";
//    protected static int port = 7080;
//    protected static boolean useSSL = false;
//    private ObjectFactory WS_OBJECT_FACTORY;
//    private WebservicesRemote WEBSERVICE_REMOTE;
//
//    @BeforeClass
//    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
//        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
//
//        //build reference variable bits
//        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
//        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
//        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);
//
//        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
//        WS_OBJECT_FACTORY = new ObjectFactory();
//    }
//
//    @BeforeTest
//    public void setup() {
//    }
//
//    @AfterTest
//    public void teardown() {
//    }
//
//    // Create and use SubjectManagerBean
//    @Test(enabled = TESTS_ENABLED)
//    public void testSubject() throws java.lang.Exception {
//
//        //
//        Subject user = WEBSERVICE_REMOTE.login(credentials, credentials);
//
//        // test ref
//        assertNotNull(user);
//
//        // test subject details: CREATE
//        assertEquals(credentials, user.getName());
//
//        // test session Id : CREATE
//        assertNotNull(user.getSessionId());
//
//        // test for invalid data in session id : CREATE
//        assertTrue(user.getSessionId() != 0);
//
//        // SUBJECT TESTING
//        // check for user that shouldn't be there. Incomplete cleanups do occur
//        // and leave db debris
//        String testUserName = "ws-test-user";
//
//        // Use Case # 7. : SEARCH
//
//        Subject newUser = WEBSERVICE_REMOTE.getSubjectByName(testUserName);
//
//        // if there, must have been from previous failed test... clean up.
//        if (null != newUser) {
//            // Integer[] list = new Integer[newUser.getId()];
//            ArrayList<Integer> list = new ArrayList<Integer>();
//            list.add(newUser.getId());
//
//            WEBSERVICE_REMOTE.deleteSubjects(user, list);
//        }
//
//        // test adding a new user with details.
//        newUser = new Subject();
//
//        String fName = "first-name";
//        String lName = "last-name";
//        newUser.setName(testUserName);
//        newUser.setFirstName(fName);
//        newUser.setLastName(lName);
//        newUser.setFactive(true);
//
//        // Send command to create the new user
//        WEBSERVICE_REMOTE.createSubject(user, newUser);
//
//        // locate the previously created user and test values
//        newUser = WEBSERVICE_REMOTE.getSubjectByName(testUserName);
//
//        assertNotNull(newUser);
//        assertTrue(testUserName.equals(newUser.getName()));
//        assertTrue(fName.equals(newUser.getFirstName()));
//        assertTrue(lName.equals(newUser.getLastName()));
//
//        // PRINCIPAL TESTING
//        // create principal
//        String testUserPass = "ws-test-password";
//
//        // create principal
//        WEBSERVICE_REMOTE.createPrincipal(user, testUserName, testUserPass);
//        // attempt login
//        newUser = WEBSERVICE_REMOTE.login(testUserName, testUserPass);
//
//        // verify login
//        WEBSERVICE_REMOTE.logout(newUser);
//
//        // test password change methods
//        WEBSERVICE_REMOTE.changePassword(user, testUserName, "updated-ws-test-password");
//        newUser = WEBSERVICE_REMOTE.login(testUserName, "updated-ws-test-password");
//
//        // assertTrue(subjectManager.isLoggedIn(testUserName));
//        WEBSERVICE_REMOTE.logout(newUser);
//
//        // locate subject
//        newUser = WEBSERVICE_REMOTE.getSubjectByName(testUserName);
//        // more subject modification testing .. this time using update()
//        newUser.setFirstName("updated-first-name");
//        newUser.setLastName("updated-last-name");
//        WEBSERVICE_REMOTE.updateSubject(user, newUser);
//
//        // checking for successful update
//        newUser = WEBSERVICE_REMOTE.getSubjectByName(testUserName);
//
//        assertTrue("ws-test-user".equals(newUser.getName()));
//        assertTrue("updated-first-name".equals(newUser.getFirstName()));
//        assertTrue("updated-last-name".equals(newUser.getLastName()));
//
//        // ROLE testing
//        // locate roles available for subject
//        PageControl pageControl = new PageControl();
//        pageControl.setPageSize(10);
//        Integer[] emptyList = null;
//
//        // Find roles possible to add. Ex. lay users cannot elevate another
//        // account to SYS ADM
//        // Access controls for Use Case #7
//        List<Role> roles = null;
//        roles = WEBSERVICE_REMOTE.findSubjectAssignedRoles(user, user.getId(), pageControl);
//        // displayXml(roles);
//
//        // locate 'All Role Id'
//        int roleId = -1;
//        for (Role role : roles) {
//            // displayXml(role);
//            if ("all resources role".equalsIgnoreCase(role.getName())) {
//                roleId = role.getId();
//                break;
//            }
//        }
//        assertTrue("Role was not successfully located.", (roleId != -1));
//
//        // assign that role to the subject
//        List<Integer> roleBag = new ArrayList<Integer>();
//        roleBag.add(roleId);
//        WEBSERVICE_REMOTE.addRolesToSubject(user, Integer.valueOf(newUser.getId()), roleBag);
//
//        // check that assignment occurred
//        List<Role> attachedSubjects = WEBSERVICE_REMOTE.findSubjectAssignedRoles(user, newUser.getId(), pageControl);
//        boolean found = false;
//        for (Role subject : attachedSubjects) {
//
//            if (subject.getId() == roleId) {
//                found = true;
//            }
//        }
//        assertTrue(found);
//
//        // now cleanup the user and association we just created.
//        List<Integer> cleanup = new ArrayList<Integer>();
//        cleanup.add(newUser.getId());
//        WEBSERVICE_REMOTE.deleteSubjects(user, cleanup);
//        assertNull(WEBSERVICE_REMOTE.getSubjectByName("ws-test-user"));
//
//    }
//
//}
