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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugins.rhnhosted.RHNProvider;

public class TestPerfPackageMetadata {
    //
    // By default this test will _NOT_ be run during normal unit test runs.
    // It's goal is to give us a means to isolate a full run of the communication & parsing of package metadata
    // with RHN Hosted, outside of a JBoss/RHQ environment
    //
    private final Log log = LogFactory.getLog(TestPerfPackageMetadata.class);
    String PROP_NAME_TO_TRIGGER_TEST = "RunTestPerfPackageMetadata";
    boolean isTesting = false;
    //String rhnURL = "http://satellite.rhn.stage.redhat.com";
    String rhnURL = "http://satellite.rhn.redhat.com";
    String certLoc = "./entitlement-cert.xml";

    @BeforeTest
    public void setUp() throws Exception {
        String value = System.getProperty(PROP_NAME_TO_TRIGGER_TEST);
        if (!StringUtils.isBlank(value)) {
            isTesting = Boolean.parseBoolean(value);
        }
    }

    @AfterTest
    public void tearDown() {
    }

    public Configuration getConfiguration() {
        String certData = "";
        try {
            certData = FileUtils.readFileToString(new File(certLoc));
        } catch (Exception e) {
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

    protected Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", "3.3");
        return reqProps;
    }

    @Test
    public void testRHEL5FetchMetadata() {

        if (!isTesting) {
            log.info("Intentionally skipping test, since property is missing: " + PROP_NAME_TO_TRIGGER_TEST);
            return;
        }
        log.info("Started test of fetching package metadata");
        String channelName = "rhel-i386-server-5";

        RHNProvider provider = new RHNProvider();
        Configuration config = getConfiguration();
        PackageSyncReport report = new PackageSyncReport();
        try {
            provider.initialize(config);
            List<ContentProviderPackageDetails> existingPackages = new ArrayList<ContentProviderPackageDetails>();
            provider.synchronizePackages(channelName, report, existingPackages);
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
        Set<ContentProviderPackageDetails> newPkgs = report.getNewPackages();
        log.info("Fetched metadata for " + newPkgs.size() + " packages.");
        provider.shutdown();
    }
}
