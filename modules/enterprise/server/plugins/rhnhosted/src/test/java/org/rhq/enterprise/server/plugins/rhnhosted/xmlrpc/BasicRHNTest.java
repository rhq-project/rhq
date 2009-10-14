package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.jaxb.JaxbTypeFactory;

import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.CustomReqPropTransportFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnJaxbTransportFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatellite;

/**
 * Unit test for checking xmlrpc communication with RHN hosted
 */
public class BasicRHNTest extends TestCase
{
    public String systemIdPath = "./src/test/resources/systemid";

    public BasicRHNTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BasicRHNTest.class );
    }

    protected Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", "3.3");
        return reqProps;
    }

    protected String getSystemId() throws Exception {
        if (new File(systemIdPath).exists() == false) {
            return "";
        }
        return FileUtils.readFileToString(new File(systemIdPath));
    }

    /**
     * Tests get version
     * */
    public void testGetVersion() throws Exception
    {
        boolean success = true;

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://xmlrpc.rhn.redhat.com/rpc/api"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            String version = (String) client.execute( "api.getVersion", new Object[] {});
            assertTrue(version.length() > 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void testAuth() throws Exception
    {
        boolean success = true;
    
        try {
            String systemid = getSystemId(); 
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            Integer result = (Integer) client.execute("authentication.check", params);
            assertTrue(result.intValue() == 1);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }


    public void testDumpChannels() throws Exception 
    {
        boolean success = true;

        try {
            String systemid = getSystemId(); 
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            client.setTransportFactory(transportFactory);
            
            JAXBContext jc = JAXBContext.newInstance("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            TypeFactory tf = new JaxbTypeFactory(client, jc);
            client.setTypeFactory(tf);

            List<String> channel_labels = new ArrayList<String>();
            channel_labels.add("rhel-i386-server-5");
            Object[] params = new Object[]{systemid, channel_labels};
            RhnSatellite sat = (RhnSatellite) client.execute("dump.channels", params);


            //System.err.println("channel name = " + sat.getRhnChannels().getRhnChannel().getRhnChannelName());
            //System.err.println("channel summary = " + sat.getRhnChannels().getRhnChannel().getRhnChannelSummary());
            String packages = sat.getRhnChannels().getRhnChannel().getPackages();
            String[] pkgIds = packages.split(" ");
            //System.err.println(pkgIds.length + " package IDs parsed.");
            //System.err.println("package[0] = " + pkgIds[0]);
            assertTrue(pkgIds.length > 1);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }


    public void DONT_RUN_testDumpPackage() throws Exception 
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            CustomReqPropTransportFactory transportFactory =  new CustomReqPropTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            client.setTransportFactory(transportFactory);
            List<String> packages = new ArrayList<String>();
            packages.add("rhn-package-386981");
            Object[] params = new Object[]{systemid, packages};
            Object result = client.execute("dump.packages_short", params);
            System.out.println("dump.packages_short = " + result);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }


}
