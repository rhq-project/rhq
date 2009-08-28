package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.ws.utility.WsUtility;

/**
 * These tests can not be executed in our standard unit test fashion as they
 * require a running RHQ Server with our web services deployed.
 * 
 * This is still in development and has the current restrictions:
 * - add [dev_root]/modules/enterprise/remoting/webservices/target/rhq-remoting-webservices-{version}.jar 
 *    to TOP of IDE classpath for development/testing. 
 * - Server running on localhost. 
 * - ws-test user defined in database with full permissions 
 * - Non RHQ Server JBossAS in inventory. 
 * - The ws.test.package-path and ws.test.package-version environment 
 *   variables must be defined to a test .war file.
 * 
 * @author Jay Shaughnessy, Simeon Pinder
 */
@Test(groups = "ws")
public class WsSupportManagerTest extends AssertJUnit implements TestPropertiesInterface{

    //Test variables
    private static ObjectFactory WS_OBJECT_FACTORY;
    private static WebservicesRemote WEBSERVICE_REMOTE;
    private static Subject subject = null;

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
        LoginException_Exception {

        //build reference variable bits
        URL gUrl = WsUtility.generateRemoteWebserviceURL(WebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRemoteWebserviceQName(WebservicesManagerBeanService.class);
        WebservicesManagerBeanService jws = new WebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
        WsSubjectTest.checkForWsTestUserAndRole();
        subject = WEBSERVICE_REMOTE.login(credentials, credentials);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testSnapshotReport() throws java.lang.Exception {
        //Locate a resource to get snapshot for
        //        ResourceCriteria criteria = createCriteria();
        //TODO: fix default criteria
        ResourceCriteria criteria = WS_OBJECT_FACTORY.createResourceCriteria();
        //TODO: Figure out how to generically call this. Will only work for the resource that represents the AGENT itself.
        //        criteria.setFilterName("Vital-AGENT RHQ Agent");
        criteria.setFilterName("AGENT RHQ Agent");
        //        criteria.setFilterName("service-alpha-0");
        //        criteria.setFilterParentResourceName("server-omega-2");
        List<Resource> resources = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, criteria);

        //Test that we have a resource to use
        assertNotNull("Resource list was null for some reason.", resources);
        assertTrue("No resources were located. Unable to proceed.", resources.size() > 0);
        assertTrue("Resource id was invalid.", resources.get(0).getId() > -1);

        int resourceId = resources.get(0).getId();
        String name = "SupportSnapshot-Agent";
        String description = "Some Description";
        //locate resource to get resourceId
        String snapshotReport = WEBSERVICE_REMOTE.getSnapshotReport(subject, resourceId, name, description);
        assertNotNull("Snapshot was not located.", snapshotReport);
        //        System.out.println("SNAPSHOTREPORT:" + snapshotReport + ":");
    }
}
