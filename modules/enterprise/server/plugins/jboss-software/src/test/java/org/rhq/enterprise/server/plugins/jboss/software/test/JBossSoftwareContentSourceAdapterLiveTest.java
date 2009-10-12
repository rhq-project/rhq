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
package org.rhq.enterprise.server.plugins.jboss.software.test;

import java.io.InputStream;
import org.testng.annotations.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.plugins.jboss.software.JBossSoftwareContentSourceAdapter;

/**
 * Simple tests that makes sure the content source adapter can connect to and parse the results of the live software
 * feed. The actual parsed results aren't verified, simply that it doesn't throw an exception at at least returns a
 * package. These tests should remain disabled while checked in. I don't know of a testing login for the CSP, so before
 * running these tests be sure to update the username and password in the configuration object. Also flip the
 * TESTS_ENABLED flag to true to actually run the tests.
 *
 * @author Jason Dobies
 */
public class JBossSoftwareContentSourceAdapterLiveTest {

    private static final boolean TESTS_ENABLED = false;

    private static final Configuration CONFIGURATION = new Configuration();

    static {
        CONFIGURATION.put(new PropertySimple(
                "url",
                "https://support.redhat.com/jbossnetwork/restricted/feed/software.html?product=all&downloadType=all&flavor=rss&version=&jonVersion=2.0"));
        CONFIGURATION.put(new PropertySimple("username", "-- ENTER USERNAME --"));
        CONFIGURATION.put(new PropertySimple("password", "-- ENTER PASSWORD --"));
        CONFIGURATION.put(new PropertySimple("active", "true"));
    }

    private static final Configuration PROXY_CONFIGURATION;

    static {
        PROXY_CONFIGURATION = CONFIGURATION.deepCopy();

        PROXY_CONFIGURATION.put(new PropertySimple("proxyUrl", "jonqa.rdu.redhat.com"));
        PROXY_CONFIGURATION.put(new PropertySimple("proxyPort", "3129"));
    }

    private static final Configuration AUTHENTICATING_PROXY_CONFIGURATION;

    static {
        AUTHENTICATING_PROXY_CONFIGURATION = PROXY_CONFIGURATION.deepCopy();

        AUTHENTICATING_PROXY_CONFIGURATION.put(new PropertySimple("proxyUsername", "squiduser"));
        AUTHENTICATING_PROXY_CONFIGURATION.put(new PropertySimple("proxyPassword", "squiduser"));
        AUTHENTICATING_PROXY_CONFIGURATION.put(new PropertySimple("proxyPort", "3128"));
    }

    private final Log log = LogFactory.getLog(this.getClass());

    // Test Cases  --------------------------------------------

    @Test(enabled = TESTS_ENABLED)
    public void liveConnection() throws Exception {
        // Setup
        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(CONFIGURATION);

        // Test
        PackageSyncReport report = new PackageSyncReport();

        adapter.synchronizePackages(report, null);

        // Verify
        assert report.getNewPackages().size() > 0 : "No packages were parsed from the live feed";

        log.info("Number of new packages found in liveConnection: " + report.getNewPackages().size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testConnection() throws Exception {
        // Setup
        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(CONFIGURATION);

        // Test

        // This will throw an exception if the connection cannot be made
        adapter.testConnection();
    }

    @Test(enabled = TESTS_ENABLED)
    public void bitsGrab() throws Exception {
        // Setup
        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(CONFIGURATION);

        // Test
        InputStream inputStream = adapter
            .getInputStream("https://network.jboss.com/jbossnetwork/secureDownload.html?softwareId=a0430000007iuElAAI");

        // Verify
        assert inputStream != null : "No input stream read from URL";
    }

    @Test(enabled = TESTS_ENABLED)
    public void proxyConnection() throws Exception {
        // Setup
        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(PROXY_CONFIGURATION);

        // Test
        PackageSyncReport report = new PackageSyncReport();

        adapter.synchronizePackages(report, null);

        // Verify
        assert report.getNewPackages().size() > 0 : "No packages were parsed from the live feed";

        log.info("Number of new packages found in proxyConnection: " + report.getNewPackages().size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void authenticatingProxyConnection() throws Exception {
        // Setup
        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(AUTHENTICATING_PROXY_CONFIGURATION);

        // Test
        PackageSyncReport report = new PackageSyncReport();

        adapter.synchronizePackages(report, null);

        // Verify
        assert report.getNewPackages().size() > 0 : "No packages were parsed from the live feed";

        log.info("Number of new packages found in proxyConnection: " + report.getNewPackages().size());
    }

    @Test(enabled = TESTS_ENABLED)
    public void invalidLogin() throws Exception {
        // Setup
        Configuration invalidLoginConfiguration = CONFIGURATION.clone();
        invalidLoginConfiguration.put(new PropertySimple("username", "foo"));

        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(invalidLoginConfiguration);

        // Test
        PackageSyncReport report = new PackageSyncReport();

        try {
            adapter.synchronizePackages(report, null);
        } catch (Exception e) {
            assert e.getMessage().toLowerCase().contains("invalid login") :
                "Error message does not properly indicate a failed login. Message: " + e.getMessage();
        }

    }

    @Test(enabled = TESTS_ENABLED)
    public void invalidUrl() throws Exception {
        // Setup
        Configuration invalidUrlConfiguration = CONFIGURATION.clone();
        invalidUrlConfiguration.put(new PropertySimple("url", "http://redhat.com/foo.html"));

        JBossSoftwareContentSourceAdapter adapter = new JBossSoftwareContentSourceAdapter();
        adapter.initialize(invalidUrlConfiguration);

        // Test
        PackageSyncReport report = new PackageSyncReport();

        try {
            adapter.synchronizePackages(report, null);
        } catch (Exception e) {
            assert e.getMessage().toLowerCase().contains("not find the feed") :
                "Error message does not properly indicate an incorrect URL. Message: " + e.getMessage();
        }

    }
}