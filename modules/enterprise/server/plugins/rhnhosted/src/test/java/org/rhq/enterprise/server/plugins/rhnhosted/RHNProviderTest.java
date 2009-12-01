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
package org.rhq.enterprise.server.plugins.rhnhosted;


import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionFileDetails;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnDownloader;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author John Matthews
 */
public class RHNProviderTest
{
    // By Default the tests in this class will be skipped.  This class is intended as a simple integration test
    // desire is for it to only run against RHN Hosted, other tests in this package will execute against a mocked RHN
    // connection.  This test is a means for us to manually check that Plugin->RHN Hosted communication is behaving as
    // we expect.
    //
    // If you want to run these tests, then set the java property "RunRHNProviderTest"
    //
    String PROP_NAME_TO_TRIGGER_TEST = "RunRHNProviderTest";
    String rhnURL = "http://satellite.rhn.redhat.com";
    String certLoc = "./entitlement-cert.xml";
    boolean isTesting = false;

    RHNProvider provider = new RHNProvider();

    @BeforeTest
    public void setUp() throws Exception
    {
        String value = System.getProperty(PROP_NAME_TO_TRIGGER_TEST);
        if (!StringUtils.isBlank(value)) {
             isTesting = Boolean.parseBoolean(value);
        }
    }

    @AfterTest
    public void tearDown()
    {
    }

    public Configuration getConfiguration() {
        String certData = "";
        try {
            certData = FileUtils.readFileToString(new File(certLoc));
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        return getConfiguration(rhnURL, certData);
    }

    public Configuration getConfiguration(String location, String certData) {
        Configuration config = new Configuration();
        PropertySimple locProp = new PropertySimple();
        locProp.setName("location");
        locProp.setStringValue(location);
        config.put(locProp);
        PropertySimple certProp = new PropertySimple();
        certProp.setName("certificate");
        certProp.setStringValue(certData);
        config.put(certProp);
        return config;
    }

    @Test
    public void testGetInputStream()
    {
        if (!isTesting) {
            System.out.println("Intentionally skipping test, since property is missing: -D" + PROP_NAME_TO_TRIGGER_TEST);
            return;
        }

        System.out.println("testGetInputStream invoked");
        //
        // systemid used for this test needs to be entitled so the channel/package selected
        //
        String cName = "rhel-x86_64-server-5";
        String rName = "openhpi-2.4.1-6.el5.1.x86_64.rpm";
        RHNProvider provider = new RHNProvider();
        Configuration config = getConfiguration();
        try {
            provider.initialize(config);
            String loc = RHNHelper.constructPackageUrl(rhnURL, cName, rName);
            //String loc = rhnURL + "/SAT/$RHN/" + cName + "/getPackage/" + rName;
            InputStream in = provider.getInputStream(loc);
            assert (in != null);
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.println("testGetInputStream finished.");
        provider.shutdown();
    }

    @Test
    public void testSynchronizePackages()
    {
        if (!isTesting) {
            System.out.println("Intentionally skipping test, since property is missing: -D" + PROP_NAME_TO_TRIGGER_TEST);
            return;
        }

        //String channelName = "rhn-tools-rhel-i386-server-5";
        String channelName = "rhel-i386-server-5";
        Configuration config = getConfiguration();
        RHNProvider provider = new RHNProvider();

        System.out.println("testSynchronizePackages invoked");
        PackageSyncReport report = new PackageSyncReport();
        List<ContentProviderPackageDetails> existingPackages = new ArrayList<ContentProviderPackageDetails>();
        try {
            provider.initialize(config);
            provider.synchronizePackages(channelName, report, existingPackages);
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        System.out.println("testSynchronizePackages finished.");
    }

    @Test
    public void testSynchronizeDistribution()
    {

        if (!isTesting) {
            System.out.println("Intentionally skipping test, since property is missing: -D" + PROP_NAME_TO_TRIGGER_TEST);
            return;
        }
        System.out.println("testSynchronizeDistribution invoked");
        String channelName = "rhel-x86_64-server-5";
        int dummyValueContentSourceId = -1;
        DistributionSyncReport report = new DistributionSyncReport(dummyValueContentSourceId);
        List<DistributionDetails> existingDistro = new ArrayList<DistributionDetails>();
        RHNProvider provider = new RHNProvider();
        Configuration config = getConfiguration();
        try {
            provider.initialize(config);
            provider.synchronizeDistribution(channelName, report, existingDistro);
        }
        catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        List<DistributionDetails> details = report.getDistributions();
        System.out.println("RHNProviderTest details.size() = " + details.size());
        for (DistributionDetails d: details) {
            System.out.println("Label = " + d.getLabel());
            System.out.println("Path = " + d.getDistributionPath());
            List<DistributionFileDetails> files = d.getFiles();
            for (DistributionFileDetails f: files) {
                System.out.println(d.getLabel() + ", " + d.getDistributionPath() + ", " +
                        f.getRelativeFilename() + " , " + f.getMd5sum() + ", lastModified = " + f.getLastModified() +
                        ", fileSize = " + f.getFileSize());
                try {
                    String url = RHNHelper.constructKickstartFileUrl(rhnURL, channelName, d.getLabel(),
                            f.getRelativeFilename());
                    InputStream in = provider.getInputStream(url);

                    String actualMd5sum = MessageDigestGenerator.getDigestString(in);
                    String expectedMd5sum = f.getMd5sum();
                    assert(StringUtils.equalsIgnoreCase(actualMd5sum, expectedMd5sum));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    assert false;
                }

            }
        }
        System.out.println("testSynchronizeDistribution finished.");
    }
}
