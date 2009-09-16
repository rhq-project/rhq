//package org.rhq.enterprise.server.ws;
//
//import java.lang.reflect.InvocationTargetException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.List;
//
//import javax.xml.namespace.QName;
//
//import org.testng.AssertJUnit;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import org.rhq.enterprise.server.ws.utility.WsUtility;
//
///**
// * These tests can not be executed in our standard unit test fashion as they
// * require a running RHQ Server with our web services deployed.
// * 
// * This is still in development and has the current restrictions: - add
// * [dev_root
// * ]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices
// * -{version}.jar to TOP of eclipse classpath to run from your IDE(actually need
// * to use classpath setup from bin/jbossas/bin/wsrunclient.sh to take advantage
// * of type substitution correctly) - Server running on localhost. - ws-test user
// * defined in database with full permissions - Non RHQ Server JBossAS in
// * inventory. - The -Ptest-ws profile specified when running mvn test from
// * webservices dir - Perftest plugin installed and agent started as described in
// * modules/enterprise/remoting/scripts/README.txt
// * 
// * @author Jay Shaughnessy, Simeon Pinder
// */
//@Test(groups = "ws")
//public class WsDataAccessManagerTest extends AssertJUnit implements TestPropertiesInterface {
//
//    // Test variables
//    private static ObjectFactory WS_OBJECT_FACTORY;
//    private static WebservicesRemote WEBSERVICE_REMOTE;
//    private static Subject subject = null;
//
//    @BeforeClass
//    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
//        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
//        LoginException_Exception {
//
//        // build reference variable bits
//        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
//        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
//        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);
//
//        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
//        WS_OBJECT_FACTORY = new ObjectFactory();
//        WsSubjectTest.checkForWsTestUserAndRole();
//        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
//    }
//
//    String query = "SELECT r " + "FROM Resource r "
//        + "WHERE ( r.inventoryStatus = org.rhq.core.domain.resource.InventoryStatus.COMMITTED "
//        + "AND LOWER( r.resourceType.name ) like 'service-alpha' "
//        + "AND LOWER( r.parentResource.name ) like 'server-omega-0')";
//
//    @Test(enabled = TESTS_ENABLED)
//    void testExecuteQuery() throws LoginException_Exception {
//        Subject admin = WEBSERVICE_REMOTE.login("rhqadmin", "rhqadmin");
//        List<AnyTypeArray> resources = WEBSERVICE_REMOTE.executeQuery(admin, query);
//
//        assertEquals("Expected to get back 10 resources", resources.size(), 10);
//    }
//
//    @Test(enabled = TESTS_ENABLED)
//    void testExecuteQueryWithPaging() throws LoginException_Exception {
//        PageControl pageControl = new PageControl();
//        pageControl.pageNumber = 0;
//        pageControl.pageSize = 5;
//        // pageControl.setPrimarySort("name", PageOrdering.ASC);
//        pageControl.setPrimarySortOrder(PageOrdering.ASC);
//
//        Subject admin = WEBSERVICE_REMOTE.login("rhqadmin", "rhqadmin");
//        List<AnyTypeArray> resources = WEBSERVICE_REMOTE.executeQueryWithPageControl(admin, query, pageControl);
//
//        assertEquals("Failed to fetch first page of resources", resources.size(), 5);
//        assertNotNull("Query result was null.", resources.get(0));
//
//        assertEquals("Failed to fetch first page of resources", resources.size(), 5);
//
//        //        assertEquals("service-alpha-0", "Failed to sort first page in ascending order", resources.get(0).name);
//        //        assertEquals("Failed to sort first page in ascending order", resources.get(4).name, "service-alpha-4");
//        //
//        //        pageControl.pageNumber = 1;
//        //        resources = WEBSERVICE_REMOTE.executeQueryWithPageControl(subject, query, pageControl);
//        //
//        //        assertEquals("Failed to fetch second page of resources", resources.size(), 5);
//        //        assertEquals(resources.get(0).name, "service-alpha-5", "Failed to sort second page in ascending order");
//        //        assertEquals(resources.get(4).name, "service-alpha-9", "Failed to sort second page in ascending order");
//
//    }
//}
