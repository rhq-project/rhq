package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSourcePackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnComm;



public class RhnCommTest extends TestCase {
    
    public String systemIdPath = "./src/test/resources/systemid";

    public RhnCommTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RhnCommTest.class);
    }

    protected String getSystemId() throws Exception {
        if (new File(systemIdPath).exists() == false) {
            return "";
        }
        return FileUtils.readFileToString(new File(systemIdPath));
    }

    protected RhnComm getRhnComm() {
        RhnComm comm = new RhnComm();
        comm.setServerURL("http://satellite.rhn.redhat.com");
        return comm;
    }

    public void testCheckAuth() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            assertTrue(comm.checkAuth(getSystemId()));
            success = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetProductNames() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<RhnProductNameType> names = comm.getProductNames(getSystemId());
            assertTrue(names != null);
            assertTrue(names.size() > 0);
            for (RhnProductNameType name: names) {
                assertFalse(StringUtils.isBlank(name.getName()));
                assertFalse(StringUtils.isBlank(name.getLabel()));
            }
            success = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetChannelFamilies() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();
            List<RhnChannelFamilyType> families = comm.getChannelFamilies(getSystemId());
            assertTrue(families != null);
            assertTrue(families.size() > 0);
            for (RhnChannelFamilyType family: families) {
                /* Note that MaxMembers, VirtSubLevelLabel, and VirtSubLevelName may be null */
                assertFalse(StringUtils.isBlank(family.getChannelLabels()));
                assertFalse(StringUtils.isBlank(family.getId()));
                assertFalse(StringUtils.isBlank(family.getLabel()));
            }
            success = true;
        }
        catch (Exception e) {
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
            for (RhnChannelType channel: channels) {
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }


    public void testGetRPM() throws Exception {
        boolean success = false;
        try {
        RhnComm comm = getRhnComm();
        String channelName = "rhel-x86_64-server-5";
        String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
        String saveFilePath = "./target/" + rpmName;
        assertTrue(comm.getRPM(getSystemId(), channelName, rpmName, saveFilePath));
        File t = new File(saveFilePath);
        assertTrue(t.exists());
        success = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }
}
