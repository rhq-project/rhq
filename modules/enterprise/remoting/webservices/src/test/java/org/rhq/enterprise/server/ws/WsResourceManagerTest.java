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

@Test(groups = "ws")
public class WsResourceManagerTest extends AssertJUnit {

    //Test variables
    private static final boolean TESTS_ENABLED = true;
    protected static String credentials = "ws-test";
    protected static String host = "127.0.0.1";
    protected static int port = 7080;
    protected static boolean useSSL = false;
    private ObjectFactory WS_OBJECT_FACTORY;
    private JonWebservicesRemote WEBSERVICE_REMOTE;

    @BeforeClass
    public void init() throws ClassNotFoundException, MalformedURLException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {

        //build reference variable bits
        URL gUrl = WsUtility.generateRhqRemoteWebserviceURL(JonWebservicesManagerBeanService.class, host, port, useSSL);
        QName gQName = WsUtility.generateRhqRemoteWebserviceQName(JonWebservicesManagerBeanService.class);
        JonWebservicesManagerBeanService jws = new JonWebservicesManagerBeanService(gUrl, gQName);

        WEBSERVICE_REMOTE = jws.getJonWebservicesManagerBeanPort();
        WS_OBJECT_FACTORY = new ObjectFactory();
    }

    @Test(enabled = TESTS_ENABLED)
    public void testResourceManager() throws java.lang.Exception {
        //define search term
        String searchTerm = "RHQ AGENT";

        //build criteria
        Subject subject = WEBSERVICE_REMOTE.login(credentials, credentials);
        ResourceCriteria resourceCriteria = WS_OBJECT_FACTORY.createResourceCriteria();
        List<Resource> results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
        assertNotNull("Results not located correctly", results);

        //without filter term, should be get *
        int totalResourcesLocated = results.size();
        //check for uninitialized server
        assertTrue("Your server does not appear to be initialized. Resource count == 0.", (totalResourcesLocated > 0));

        //add criterion .. and resubmit
        resourceCriteria.setFilterName(searchTerm);
        results = WEBSERVICE_REMOTE.findResourcesByCriteria(subject, resourceCriteria);
        assertNotNull("Results not located correctly", results);
        assertTrue("Criteria did not filter properly.", (totalResourcesLocated > results.size()));
    }

}
