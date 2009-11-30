/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.BaseRHNTest;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnErratumType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFileType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFilesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;

public class RhnCommTest extends BaseRHNTest {

    public String serverUrl = "http://satellite.rhn.redhat.com";

    public static Test suite() {
        return new TestSuite(RhnCommTest.class);
    }

    protected RhnComm getRhnComm() {
        RhnComm comm = new RhnComm(serverUrl);
        return comm;
    }

    /**
     * method to find a file relative to the calling class.  primarily
     * useful when writing tests that need access to external data.
     * this lets you put the data relative to the test class file.
     *
     * @param path the path, relative to caller's location
     * @return URL a URL referencing the file
     * @throws ClassNotFoundException if the calling class can not be found
     * (i.e., should not happen)
     */
    public static URL findTestData(String path) throws ClassNotFoundException {
        Throwable t = new Throwable();
        StackTraceElement[] ste = t.getStackTrace();

        String className = ste[1].getClassName();
        Class clazz = Class.forName(className);

        URL ret = clazz.getResource(path);
        return ret;
    }

    public void testCheckAuth() throws Exception {
        URL f = findTestData("RhnCommTest.class");
        System.out.println("foo: " + f);
        RhnDownloader downloader = new RhnDownloader(serverUrl);
        assertTrue(downloader.checkAuth(SYSTEM_ID));
    }

    public void testCheckAuth_withBadSystemId() throws Exception {
        boolean success = false;
        RhnDownloader downloader = new RhnDownloader(serverUrl);
        try {
            boolean result = downloader.checkAuth(SYSTEM_ID_BAD);
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testCheckAuth_withBadURL() throws Exception {
        boolean success = false;
        RhnDownloader downloader = new RhnDownloader("http://badurl.example.com/BAD");
        try {
            boolean result = downloader.checkAuth(SYSTEM_ID);
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
        if (StringUtils.isBlank(SYSTEM_ID)) {
            System.out.println("Skipping test since systemid is not readable");
            return;
        }

        RhnComm comm = getRhnComm();
        List<RhnProductNameType> names = comm.getProductNames(SYSTEM_ID);
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
            List<RhnChannelFamilyType> families = comm.getChannelFamilies(SYSTEM_ID);
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
            throw (e);
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
            List<RhnChannelType> channels = comm.getChannels(SYSTEM_ID, channel_labels);
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
            throw (e);
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
            List<RhnPackageShortType> pkgs = comm.getPackageShortInfo(SYSTEM_ID, reqPackages);
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
            throw (e);
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

            List<RhnKickstartableTreeType> ksTrees = comm.getKickstartTreeMetadata(SYSTEM_ID, reqLabels);
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
            throw (e);
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
            List<RhnPackageType> pkgs = comm.getPackageMetadata(SYSTEM_ID, reqPackages);
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
            List<RhnPackageType> pkgs = comm.getPackageMetadata(SYSTEM_ID_BAD, reqPackages);
            assertTrue(false);
        } catch (XmlRpcException e) {
            assertTrue(e.getMessage().contains("Invalid System Credentials"));
            assertTrue(e.code == -9);
            success = true;
        }
        assertTrue(success);
    }

    public void testLogin() throws Exception {
        RhnDownloader comm = new RhnDownloader(serverUrl);
        Map credentials = comm.login(SYSTEM_ID);
        assertNotNull(credentials);
    }

    public void testGetRPM() throws Exception {
        boolean success = false;
        try {
            RhnDownloader comm = new RhnDownloader(serverUrl);
            String channelName = "rhel-x86_64-server-5";
            String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
            String saveFilePath = "./target/" + rpmName;
            assertTrue(comm.getRPM(SYSTEM_ID, channelName, rpmName, saveFilePath));
            File t = new File(saveFilePath);
            assertTrue(t.exists());
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw (e);
        }
        assertTrue(success);
    }

    public void testGetRPM_withBadSystemId() throws Exception {
        String prop = System.getProperty(RhnHttpURLConnectionFactory.RHN_MOCK_HTTP_URL_CONNECTION);
        if (!StringUtils.isEmpty(prop)) {
            System.err.println("Skipping testGetRPM_withBadSystemId() since we are mocking the HTTP connection.");
            return;
        }

        boolean success = false;
        try {
            RhnDownloader comm = new RhnDownloader(serverUrl);
            String channelName = "rhel-x86_64-server-5";
            String rpmName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
            String saveFilePath = "./target/" + rpmName;
            assertTrue(comm.getRPM(SYSTEM_ID_BAD, channelName, rpmName, saveFilePath));
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
            List<RhnKickstartableTreeType> ksTrees = comm.getKickstartTreeMetadata(SYSTEM_ID, reqLabels);
            RhnKickstartableTreeType tree = ksTrees.get(0);
            RhnKickstartFilesType ksFiles = tree.getRhnKickstartFiles();
            System.err.println("ksFiles = " + ksFiles);
            List<RhnKickstartFileType> files = ksFiles.getRhnKickstartFile();
            RhnKickstartFileType f = files.get(0);
            //Only fetching one kickstart file to save time.
            //To do a more exhaustive test, simply interate of ks.getRhnKickstartFile()
            //and fetch each file, this will take a few minutes to complete.
            String ksRelativePath = f.getRelativePath();
            assertFalse(StringUtils.isBlank(ksRelativePath));
            System.err.println("fetching ks file: " + f.getRelativePath());
            RhnDownloader downloader = new RhnDownloader(serverUrl);
            InputStream in = downloader.getKickstartTreeFile(SYSTEM_ID, channelName, ksTreeLabel, ksRelativePath);
            assertTrue(in != null);
            in.close();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(success);
    }

    public void testGetErrata() throws Exception {
        boolean success = false;
        try {
            RhnComm comm = getRhnComm();

            List<String> errataIds = new ArrayList<String>();

            errataIds.add("rhn-erratum-6183");
            errataIds.add("rhn-erratum-6184");
            errataIds.add("rhn-erratum-6182");
            List<RhnErratumType> errata = comm.getErrataMetadata(SYSTEM_ID, errataIds);
            assertTrue(errata.size() == errataIds.size());
            for (RhnErratumType e : errata) {
                assertFalse(StringUtils.isBlank(e.getAdvisory()));
                assertFalse(StringUtils.isBlank(e.getChannels()));
                assertFalse(StringUtils.isBlank(e.getPackages()));
                assertFalse(StringUtils.isBlank(e.getRhnErratumAdvisoryName()));
                assertFalse(StringUtils.isBlank(e.getRhnErratumAdvisoryType()));
                assertFalse(StringUtils.isBlank(e.getRhnErratumDescription()));
                assertFalse(StringUtils.isBlank(e.getRhnErratumSynopsis()));

            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
