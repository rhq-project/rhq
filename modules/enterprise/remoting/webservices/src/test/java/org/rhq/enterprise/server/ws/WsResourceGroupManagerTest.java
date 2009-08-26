package org.rhq.enterprise.server.ws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
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
public class WsResourceGroupManagerTest extends AssertJUnit implements TestPropertiesInterface {

    //Test variables
//    private static final boolean TESTS_ENABLED = true;
//    protected static String credentials = "ws-test";
//    protected static String host = "127.0.0.1";
//    protected static int port = 7080;
//    protected static boolean useSSL = false;
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
    public void testResourceGroup() throws java.lang.Exception {
    	
    	assertNotNull("Webservice Remote is null.",WEBSERVICE_REMOTE);
    	assertNotNull("JAXB ObjectFactory is null.",WS_OBJECT_FACTORY);
    	assertNotNull("You have not logged in successfully.",subject);
    	
    	//locate group
    	ResourceGroupCriteria groupCriteria = WS_OBJECT_FACTORY.createResourceGroupCriteria();
    	groupCriteria.setFilterName("All Perf Test Servers");
    	List<ResourceGroup> groups = WEBSERVICE_REMOTE.findResourceGroupsByCriteria(subject, groupCriteria);
    	assertNotNull("The ResourceGroup reference was null.",groups);
    	assertTrue("Group was not located.",groups.size()>0);
    	//
    	
    }
}
