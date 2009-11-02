package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSourcePackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.CustomReqPropTransportFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnComm;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnJaxbTransportFactory;

/**
 * Unit test for checking xmlrpc communication with RHN hosted
 */
public class BasicRHNTest extends TestCase
{
    public String systemIdPath = "./src/test/resources/systemid";
    public String badSystemIdPath = "./src/test/resources/systemid-BAD-ID";

    protected String serverUrl = "http://satellite.rhn.redhat.com";
    protected boolean debugDumpFile = true;

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
        return getSystemId(systemIdPath);
    }

    protected String getSystemId(String path) throws Exception {
        if (new File(path).exists() == false) {
            return "";
        }
        return FileUtils.readFileToString(new File(path));
    }

    /**
     * Tests get version
     * */
    public void testGetVersion() throws Exception
    {
        boolean success = true;

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl +  "/rpc/api"));
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

    public void testAuthenticationLogin() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId(); 
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            Map result = (Map) client.execute("authentication.login", params);
            for (Object key: result.keySet()) {
                System.err.println("Header: " + key + " = " + result.get(key));
            }
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
            config.setServerURL(new URL(serverUrl + "/SAT"));
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

    public void testAuth_BadSystemId() throws Exception
    {
        boolean success = false;

        try {
            String systemid = getSystemId(badSystemIdPath);
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            Integer result = (Integer) client.execute("authentication.check", params);
            assertTrue(false); // We shouldn't reach here, as an exception should be thrown
        }
        catch (XmlRpcException e) {
            success = true;
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
        }
        assertTrue(success);
    }


    public void testDumpProductNames() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.product_names.xml");
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.product_names", params);
            RhnSatelliteType sat = result.getValue();
            List<RhnProductNameType> names = sat.getRhnProductNames().getRhnProductName();
            assertTrue(names.size() > 0);
            for (RhnProductNameType name: names) {
                assertFalse(StringUtils.isBlank(name.getName()));
                assertFalse(StringUtils.isBlank(name.getLabel()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void testJaxbCallWithBadSystemId() throws Exception
    {
        // Point of this test is to use a systemid which is not valid
        // We want to see an exception thrown from the xmlrpc code with a proper
        // message and error code
        boolean success = false;

        try {
            String systemid = getSystemId(badSystemIdPath);
            //String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.product_names.xml");
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.product_names", params);
            assertTrue(false); //We should never get here, an exception should be thrown
        }
        catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        }
        assertTrue(success);
    }
    public void testDumpChannelFamilies() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.channel_families.xml");
            client.setTransportFactory(transportFactory);

            Object[] params = new Object[]{systemid};
            JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.channel_families", params);
            RhnSatelliteType sat = result.getValue();
            List<RhnChannelFamilyType> families = sat.getRhnChannelFamilies().getRhnChannelFamily();
            for (RhnChannelFamilyType family: families) {
                /* Note that MaxMembers, VirtSubLevelLabel, and VirtSubLevelName may be null */
                assertFalse(StringUtils.isBlank(family.getChannelLabels()));
                assertFalse(StringUtils.isBlank(family.getId()));
                assertFalse(StringUtils.isBlank(family.getLabel()));
            }
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
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.channels.xml");
            client.setTransportFactory(transportFactory);

            List<String> channel_labels = new ArrayList<String>();
            channel_labels.add("rhel-i386-server-5");
            channel_labels.add("rhn-tools-rhel-i386-server-5");
            channel_labels.add("rhel-x86_64-server-5");
            channel_labels.add("rhn-tools-rhel-x86_64-server-5");
            Object[] params = new Object[]{systemid, channel_labels};
            JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.channels", params);
            RhnSatelliteType sat = result.getValue();

            List<RhnChannelType> channels = sat.getRhnChannels().getRhnChannel();
            for (RhnChannelType channel: channels) {
                assertFalse(StringUtils.isBlank(channel.getRhnChannelName()));
                assertFalse(StringUtils.isBlank(channel.getRhnChannelSummary()));
                String packages = channel.getPackages();
                assertFalse(StringUtils.isBlank(packages));
                String[] pkgIds = packages.split(" ");
                assertTrue(pkgIds.length > 1);
            }
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
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.packages_short.xml");
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
            assertTrue(pkgs.size() == reqPackages.size());

            for (RhnPackageShortType pkgShort: pkgs) {
                assertFalse(StringUtils.isBlank(pkgShort.getId()));
                assertFalse(StringUtils.isBlank(pkgShort.getName()));
                assertFalse(StringUtils.isBlank(pkgShort.getVersion()));
                assertFalse(StringUtils.isBlank(pkgShort.getRelease()));
                assertFalse(StringUtils.isBlank(pkgShort.getPackageSize()));
                assertFalse(StringUtils.isBlank(pkgShort.getMd5Sum()));
                assertFalse(StringUtils.isBlank(pkgShort.getLastModified()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void testDumpPackages() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.packages.xml");
            client.setTransportFactory(transportFactory);

            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            Object[] params = new Object[]{systemid, reqPackages};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages", params);
            RhnSatelliteType sat = result.getValue();

            List<RhnPackageType> pkgs = sat.getRhnPackages().getRhnPackage();
            assertTrue(pkgs.size() == reqPackages.size());

            for (RhnPackageType pkg: pkgs) {
                assertFalse(StringUtils.isBlank(pkg.getRhnPackageSummary()));
                assertFalse(StringUtils.isBlank(pkg.getRhnPackageDescription()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void testDumpSourcePackages() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.source_packages.xml");
            client.setTransportFactory(transportFactory);

            List<String> reqPackages = new ArrayList<String>();
            // To get data for this call, look at channels <source-packages>
            reqPackages.add("rhn-source-package-41215");
            reqPackages.add("rhn-source-package-41217");
            reqPackages.add("rhn-source-package-41218");
            reqPackages.add("rhn-source-package-41228");
            Object[] params = new Object[]{systemid, reqPackages};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.source_packages", params);
            RhnSatelliteType sat = result.getValue();

            List<RhnSourcePackageType> pkgs = sat.getRhnSourcePackages().getRhnSourcePackage();
            assertTrue(pkgs.size() == reqPackages.size());

            for (RhnSourcePackageType pkg: pkgs) {
                assertFalse(StringUtils.isBlank(pkg.getBuildTime()));
                assertFalse(StringUtils.isBlank(pkg.getId()));
                assertFalse(StringUtils.isBlank(pkg.getLastModified()));
                assertFalse(StringUtils.isBlank(pkg.getLastModified()));
                assertFalse(StringUtils.isBlank(pkg.getMd5Sum()));
                assertFalse(StringUtils.isBlank(pkg.getPackageSize()));
                assertFalse(StringUtils.isBlank(pkg.getPayloadSize()));
                assertFalse(StringUtils.isBlank(pkg.getRpmVersion()));
                assertFalse(StringUtils.isBlank(pkg.getSourceRpm()));
                System.err.println("SourceRPM = " + pkg.getSourceRpm());
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    public void testDumpKickstartableTrees() throws Exception
    {
        boolean success = true;

        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/SAT-DUMP"));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-dump.kickstartable_trees.xml");
            client.setTransportFactory(transportFactory);

            List<String> reqLabels = new ArrayList<String>();
            // To get data for this call, look at channels kickstartable-trees=""
            reqLabels.add("ks-rhel-i386-server-5");
            reqLabels.add("ks-rhel-i386-server-5-u1");
            reqLabels.add("ks-rhel-i386-server-5-u2");
            reqLabels.add("ks-rhel-i386-server-5-u3");
            reqLabels.add("ks-rhel-i386-server-5-u4");
            Object[] params = new Object[]{systemid, reqLabels};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.kickstartable_trees", params);
            RhnSatelliteType sat = result.getValue();

            List<RhnKickstartableTreeType> trees = sat.getRhnKickstartableTrees().getRhnKickstartableTree();

            for (RhnKickstartableTreeType t: trees) {
                assertFalse(StringUtils.isBlank(t.getBasePath()));
                assertFalse(StringUtils.isBlank(t.getBootImage()));
                assertFalse(StringUtils.isBlank(t.getChannel()));
                assertFalse(StringUtils.isBlank(t.getInstallTypeLabel()));
                assertFalse(StringUtils.isBlank(t.getInstallTypeName()));
                assertFalse(StringUtils.isBlank(t.getKstreeTypeLabel()));
                assertFalse(StringUtils.isBlank(t.getKstreeTypeName()));
                assertFalse(StringUtils.isBlank(t.getLabel()));
                assertFalse(StringUtils.isBlank(t.getLastModified()));
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    /**
    Sample info
        handler =  /SAT/$RHN/rhel-x86_64-server-5/getPackage/ipsec-tools-0.6.5-6.x86_64.rpm
        header:  X-RHN-Auth-Server-Time = 1255722893.21
        header:  X-Client-Version = 1
        header:  X-RHN-Transport-Capability = ['follow-redirects=2']
        header:  X-RHN-Server-Id = XXXXXXXXXX
        header:  X-RHN-Auth = zVS7ood7Vg4I+xb2yZ6PJA==
        header:  X-RHN-Auth-User-Id =
        header:  X-RHN-Auth-Expire-Offset = 7200.0
        header:  X-Info = ['RPC Processor (C) Red Hat, Inc (version 135431)']
    */
    public void REQUIRES_AUTHtestGetPackageHTTPClient() throws Exception {
        boolean success = true;
        try {
            String systemid = getSystemId();
            if (StringUtils.isBlank(systemid)) {
                System.out.println("Skipping test since systemid is not readable");
                return;
            }
            
            String extra = "/SAT/$RHN/rhel-x86_64-server-5/getPackage/openhpi-2.4.1-6.el5.1.x86_64.rpm";
            URL url = new URL(serverUrl + extra);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("X-Client-Version", "1");
            conn.setRequestProperty("X-RHN-Server-Id", "XXXXXXXXXX");
            conn.setRequestProperty("X-RHN-Auth",  "zVS7ood7Vg4I+xb2yZ6PJA==");
            conn.setRequestProperty("X-RHN-Auth-User-Id", "");
            conn.setRequestProperty("X-RHN-Auth-Expire-Offset", "7200.0");
            conn.setRequestProperty("X-RHN-Auth-Server-Time", "1255722893.21");

            conn.setRequestMethod("GET");
            conn.connect();
            InputStream in = conn.getInputStream();


            String rpmFileName = "/tmp/getPackage-rpmFile.rpm";
            OutputStream out = new FileOutputStream(rpmFileName);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            conn.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);

    }
}
