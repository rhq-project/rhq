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
import javax.xml.bind.JAXBElement;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.jaxb.JaxbTypeFactory;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.CustomReqPropTransportFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnJaxbTransportFactory;

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

    public void XXtestAuth() throws Exception
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

            List<String> channel_labels = new ArrayList<String>();
            channel_labels.add("rhel-i386-server-5");
            Object[] params = new Object[]{systemid, channel_labels};
            JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.channels", params);
            RhnSatelliteType sat = result.getValue();


            /*
            System.err.println("channel name = " + sat.getRhnChannels().getRhnChannel().getRhnChannelName());
            System.err.println("channel summary = " + sat.getRhnChannels().getRhnChannel().getRhnChannelSummary());
            */

            String packages = sat.getRhnChannels().getRhnChannel().getPackages();
            String[] pkgIds = packages.split(" ");

            ///*
            System.err.println(pkgIds.length + " package IDs parsed.");
            System.err.println("package[0] = " + pkgIds[0]);
            System.err.println("package[1] = " + pkgIds[1]);
            System.err.println("package[2] = " + pkgIds[2]);
            System.err.println("package[3] = " + pkgIds[3]);
            //*/
            assertTrue(pkgIds.length > 1);
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }


    public void testDumpPackageShort() throws Exception
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
            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            Object[] params = new Object[]{systemid, reqPackages};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages_short", params);
            RhnSatelliteType sat = result.getValue();
            List<RhnPackageShortType> pkgs = sat.getRhnPackagesShort().getRhnPackageShort();
            System.err.println(pkgs.size() + " packages were returned.");
            assertTrue(pkgs.size() == reqPackages.size());

            for (RhnPackageShortType pkgShort: pkgs) {
                System.err.println("Id: " + pkgShort.getId());
                System.err.println("Name: " + pkgShort.getName());
                System.err.println("Epoch: " + pkgShort.getEpoch());
                System.err.println("Version: " + pkgShort.getVersion());
                System.err.println("Release: " + pkgShort.getRelease());
                System.err.println("PackageArch: " + pkgShort.getPackageArch());
                System.err.println("PackageSize: " + pkgShort.getPackageSize());
                System.err.println("Md5Sum: " + pkgShort.getMd5Sum());
                System.err.println("LastModified: " + pkgShort.getLastModified() + "\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void XXtestDumpPackages() throws Exception
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
            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            Object[] params = new Object[]{systemid, reqPackages};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages", params);
            RhnSatelliteType sat = result.getValue();
            List<RhnPackageShortType> pkgs = sat.getRhnPackagesShort().getRhnPackageShort();
            System.err.println(pkgs.size() + " packages were returned.");
            assertTrue(pkgs.size() == reqPackages.size());

            for (RhnPackageShortType pkgShort: pkgs) {
                System.err.println("Id: " + pkgShort.getId());
                System.err.println("Name: " + pkgShort.getName());
                System.err.println("Epoch: " + pkgShort.getEpoch());
                System.err.println("Version: " + pkgShort.getVersion());
                System.err.println("Release: " + pkgShort.getRelease());
                System.err.println("PackageArch: " + pkgShort.getPackageArch());
                System.err.println("PackageSize: " + pkgShort.getPackageSize());
                System.err.println("Md5Sum: " + pkgShort.getMd5Sum());
                System.err.println("LastModified: " + pkgShort.getLastModified() + "\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

}
