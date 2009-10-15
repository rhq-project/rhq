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

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSourcePackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.CustomReqPropTransportFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnJaxbTransportFactory;

/**
 * Unit test for checking xmlrpc communication with RHN hosted
 */
public class BasicRHNTest extends TestCase
{
    public String systemIdPath = "./src/test/resources/systemid";
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
                System.err.println("Channel Label = " + channel.getLabel());
                System.err.println("Channel Name = " + channel.getRhnChannelName());
                System.err.println(pkgIds.length + " package IDs parsed.");
                System.err.println("package[0] = " + pkgIds[0]);
                System.err.println("package[1] = " + pkgIds[1]);
                System.err.println("package[2] = " + pkgIds[2]);
                System.err.println("package[3] = " + pkgIds[3]);
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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
            config.setServerURL(new URL("http://satellite.rhn.redhat.com/SAT-DUMP"));
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

    public void BROKENtestDumpGetRPM() throws Exception
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
            RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
            transportFactory.setRequestProperties(getRequestProperties());
            transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
            transportFactory.setDumpMessageToFile(debugDumpFile);
            transportFactory.setDumpFilePath("/tmp/sample-rhnhosted-package.get.xml");
            client.setTransportFactory(transportFactory);

            List<String> nvrea = new ArrayList<String>();
            nvrea.add("vim");
            nvrea.add("7.0.109");
            nvrea.add("6.el5");
            nvrea.add("");
            nvrea.add("i386");
            // Call expects: systemID, channel label, (name, version, release, epoch, arch)
            Object[] params = new Object[]{systemid, "rhel-i386-server-5", nvrea};
            JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("package.get", params);
            RhnSatelliteType sat = result.getValue();
        }
        catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

}
