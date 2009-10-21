package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFileType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFilesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;

public class RhnCommTest extends TestCase {

    // Assumption is that the systemid file used is registered to RHN Hosted and it is a registered satellite
    public String[] systemIdPath = { "./src/test/resources/systemid", "/etc/sysconfig/rhn/systemid" };
    public String badSystemIdPath = "./src/test/resources/systemid-BAD-ID";
    public String serverUrl = "http://satellite.rhn.redhat.com";

    public RhnCommTest(String testName) {
        super(testName);
        System.setProperty(ApacheXmlRpcExecutor.class.getName(), MockRhnXmlRpcExecutor.class.getName());
    }

    public static Test suite() {
        return new TestSuite(RhnCommTest.class);
    }

    protected String getSystemId() throws Exception {
        for (String path : systemIdPath) {
            String data = getSystemId(path);
            if (!StringUtils.isBlank(data)) {
                return data;
            }
        }
        return "";
    }

    protected String getSystemId(String path) throws Exception {
        if (new File(path).exists()) {
            return FileUtils.readFileToString(new File(path));
        }
        return "";
    }

    protected RhnComm getRhnComm() {
        RhnComm comm = new RhnComm(serverUrl);
        return comm;
    }

    public void testCheckAuth() throws Exception {
        RhnDownloader downloader = new RhnDownloader(serverUrl);
        assertFalse(downloader.checkAuth("not a systemid"));
        assertTrue(downloader.checkAuth(getSystemId()));
    }

    public void testCheckAuth_withBadSystemId() throws Exception {
        boolean success = false;
        RhnDownloader downloader = new RhnDownloader(serverUrl);
        try {
            boolean result = downloader.checkAuth(getSystemId(badSystemIdPath));
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        }
        assertTrue(success);
    }

    public void testCheckAuth_withBadURL() throws Exception {
        boolean success = false;
        RhnDownloader downloader = new RhnDownloader("http://badurl.example.com/BAD");
        try {
            boolean result = downloader.checkAuth(getSystemId());
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Failed to read server's response"));
            assertTrue(e.code == 0);
            success = true;
        }
        assertTrue(success);
    }

    public void testGetProductNames() throws Exception {
        boolean success = false;
        if (StringUtils.isBlank(getSystemId())) {
            System.out.println("Skipping test since systemid is not readable");
            return;
        }

        RhnComm comm = getRhnComm();
        List<RhnProductNameType> names = comm.getProductNames(getSystemId());
        assertTrue(names != null);
        assertTrue(names.size() > 0);
        for (RhnProductNameType name : names) {
            assertFalse(StringUtils.isBlank(name.getName()));
            assertFalse(StringUtils.isBlank(name.getLabel()));
        }
        success = true;
        assertTrue(success);
    }

    public void testGetChannelFamilies() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<RhnChannelFamilyType> families = comm.getChannelFamilies(getSystemId());
            assertTrue(families != null);
            assertTrue(families.size() > 0);
            for (RhnChannelFamilyType family : families) {
                /* Note that MaxMembers, VirtSubLevelLabel, and VirtSubLevelName may be null */
                assertFalse(StringUtils.isBlank(family.getChannelLabels()));
                assertFalse(StringUtils.isBlank(family.getId()));
                assertFalse(StringUtils.isBlank(family.getLabel()));
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetChannels() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<String> channel_labels = new ArrayList<String>();
            channel_labels.add("rhel-i386-server-5");
            channel_labels.add("rhn-tools-rhel-i386-server-5");
            channel_labels.add("rhel-x86_64-server-5");
            channel_labels.add("rhn-tools-rhel-x86_64-server-5");
            List<RhnChannelType> channels = comm.getChannels(getSystemId(), channel_labels);
            assertTrue(channels != null);
            assertTrue(channels.size() > 0);
            for (RhnChannelType channel : channels) {
                assertFalse(StringUtils.isBlank(channel.getRhnChannelName()));
                assertFalse(StringUtils.isBlank(channel.getRhnChannelSummary()));
                String packages = channel.getPackages();
                assertFalse(StringUtils.isBlank(packages));
                String[] pkgIds = packages.split(" ");
                assertTrue(pkgIds.length > 1);
                System.err.println("testGetChannels: number of packages " + pkgIds.length);
                System.err.println("testGetChannels: " + pkgIds[0]);
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetPackageShortInfo() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            List<RhnPackageShortType> pkgs = comm.getPackageShortInfo(getSystemId(), reqPackages);
            assertTrue(pkgs.size() == reqPackages.size());
            for (RhnPackageShortType pkgShort : pkgs) {
                assertFalse(StringUtils.isBlank(pkgShort.getId()));
                assertFalse(StringUtils.isBlank(pkgShort.getName()));
                assertFalse(StringUtils.isBlank(pkgShort.getVersion()));
                assertFalse(StringUtils.isBlank(pkgShort.getRelease()));
                assertFalse(StringUtils.isBlank(pkgShort.getPackageSize()));
                assertFalse(StringUtils.isBlank(pkgShort.getMd5Sum()));
                assertFalse(StringUtils.isBlank(pkgShort.getLastModified()));
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetKickstartTreeMetadata() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<String> reqLabels = new ArrayList<String>();
            // To get data for this call, look at channels kickstartable-trees=""
            reqLabels.add("ks-rhel-i386-server-5");
            reqLabels.add("ks-rhel-i386-server-5-u1");
            reqLabels.add("ks-rhel-i386-server-5-u2");
            reqLabels.add("ks-rhel-i386-server-5-u3");
            reqLabels.add("ks-rhel-i386-server-5-u4");

            List<RhnKickstartableTreeType> ksTrees = comm.getKickstartTreeMetadata(getSystemId(), reqLabels);
            assertTrue(reqLabels.size() == ksTrees.size());
            for (RhnKickstartableTreeType tree : ksTrees) {
                assertFalse(StringUtils.isBlank(tree.getBasePath()));
                assertFalse(StringUtils.isBlank(tree.getBootImage()));
                assertFalse(StringUtils.isBlank(tree.getChannel()));
                assertFalse(StringUtils.isBlank(tree.getInstallTypeLabel()));
                assertFalse(StringUtils.isBlank(tree.getInstallTypeName()));
                assertFalse(StringUtils.isBlank(tree.getKstreeTypeLabel()));
                assertFalse(StringUtils.isBlank(tree.getKstreeTypeName()));
                assertFalse(StringUtils.isBlank(tree.getLabel()));
                assertFalse(StringUtils.isBlank(tree.getLastModified()));
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetPackageMetada() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            List<RhnPackageType> pkgs = comm.getPackageMetadata(getSystemId(), reqPackages);
            assertTrue(pkgs.size() == reqPackages.size());
            for (RhnPackageType pkg : pkgs) {
                assertFalse(StringUtils.isBlank(pkg.getRhnPackageSummary()));
                assertFalse(StringUtils.isBlank(pkg.getRhnPackageDescription()));
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetPackageMetada_withBadSystemId() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<String> reqPackages = new ArrayList<String>();
            reqPackages.add("rhn-package-386981");
            reqPackages.add("rhn-package-386982");
            reqPackages.add("rhn-package-386983");
            reqPackages.add("rhn-package-386984");
            List<RhnPackageType> pkgs = comm.getPackageMetadata(getSystemId(badSystemIdPath), reqPackages);
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        }
        assertTrue(success);
    }

    public void testGetRPM() throws Exception {
        boolean success = false;
        try {
            RhnDownloader comm = new RhnDownloader(serverUrl);
            String channelName = "rhel-x86_64-server-5";
            String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
            String saveFilePath = "./target/" + rpmName;
            assertTrue(comm.getRPM(getSystemId(), channelName, rpmName, saveFilePath));
            File t = new File(saveFilePath);
            assertTrue(t.exists());
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetRPM_withBadSystemId() throws Exception {
        boolean success = false;
        try {
            RhnDownloader comm = new RhnDownloader(serverUrl);
            String channelName = "rhel-x86_64-server-5";
            String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
            String saveFilePath = "./target/" + rpmName;
            assertTrue(comm.getRPM(getSystemId(badSystemIdPath), channelName, rpmName, saveFilePath));
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        }
        assertTrue(success);
    }

    public void testGetKickstartTree() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = new RhnComm(serverUrl);
            String channelName = "rhel-i386-server-5";
            String ksTreeLabel = "ks-rhel-i386-server-5";
            List<String> reqLabels = new ArrayList<String>();
            // To get data for this call, look at channels kickstartable-trees=""
            reqLabels.add("ks-rhel-i386-server-5");
            List<RhnKickstartableTreeType> ksTrees = comm.getKickstartTreeMetadata(getSystemId(), reqLabels);
            RhnKickstartableTreeType tree = ksTrees.get(0);
            RhnKickstartFilesType ksFiles = tree.getRhnKickstartFiles();
            List<RhnKickstartFileType> files = ksFiles.getRhnKickstartFile();
            RhnKickstartFileType f = files.get(0);
            //Only fetching one kickstart file to save time.
            //To do a more exhaustive test, simply interate of ks.getRhnKickstartFile()
            //and fetch each file, this will take a few minutes to complete.
            String ksRelativePath = f.getRelativePath();
            assertFalse(StringUtils.isBlank(ksRelativePath));
            System.err.println("fetching ks file: " + f.getRelativePath());
            RhnDownloader downloader = new RhnDownloader(serverUrl);
            InputStream in = downloader.getKickstartTreeFile(getSystemId(), channelName, ksTreeLabel, ksRelativePath);
            assertTrue(in != null);
            in.close();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }
}
