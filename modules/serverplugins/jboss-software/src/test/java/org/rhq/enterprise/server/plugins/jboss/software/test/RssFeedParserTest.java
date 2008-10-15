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

import churchillobjects.rss4j.RssDocument;
import churchillobjects.rss4j.parser.RssParser;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Set;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.enterprise.server.plugins.jboss.software.RssFeedParser;

/**
 * @author Jason Dobies
 */
public class RssFeedParserTest {
    // Constants  --------------------------------------------

    public static final String EXAMPLE_FILE_1 = "rss-feed-example-1.txt";
    public static final String EXAMPLE_FILE_2 = "rss-feed-example-2.txt";

    // Attributes  --------------------------------------------

    private RssFeedParser parser = new RssFeedParser();
    private RssDocument rssDocument1;
    private RssDocument rssDocument2;

    // Setup  --------------------------------------------

    @BeforeMethod
    public void loadRssDocument() throws Exception {
        // Example set 1
        URL exampleFeedUrl = this.getClass().getClassLoader().getResource(EXAMPLE_FILE_1);
        assert exampleFeedUrl != null : "Could not load " + EXAMPLE_FILE_1;

        rssDocument1 = loadDocument(exampleFeedUrl);

        // Example set 2
        exampleFeedUrl = this.getClass().getClassLoader().getResource(EXAMPLE_FILE_2);
        assert exampleFeedUrl != null : "Coult not load " + EXAMPLE_FILE_2;

        rssDocument2 = loadDocument(exampleFeedUrl);
    }

    // Test Cases  --------------------------------------------

    /**
     * Tests parsing into the domain model when there is no prior existing packages.
     *
     * @throws Exception if any errors are hit
     */
    @Test
    public void parse() throws Exception {
        // Setup
        PackageSyncReport report = new PackageSyncReport();

        assert rssDocument1 != null : "Rss Document was not read properly in set up";

        // Test #1 - Initial call with no prior server knowledge
        parser.parseResults(rssDocument1, report, null);

        // Verify
        Set<ContentSourcePackageDetails> newPackages = report.getNewPackages();

        assert newPackages.size() == 59 : "Incorrect number of cumulative patches found. Expected: 59, Found: "
            + newPackages.size();
        assert report.getDeletedPackages().size() == 0 : "Incorrect number of deleted packages. Expected: 0, Found: "
            + report.getDeletedPackages().size();
        assert report.getUpdatedPackages().size() == 0 : "Incorrect number of updated packages. Expected: 0, Found: "
            + report.getUpdatedPackages().size();

        int totalNumVersions = 0;
        for (ContentSourcePackageDetails pkg : newPackages) {
            totalNumVersions += pkg.getResourceVersions().size();
        }

        assert totalNumVersions == 282 : "Incorrect number of total versions represented. Expected: 282, Found: "
            + totalNumVersions;

        // Test #2 - Call again using the previous results as the existing package list, simulating the server passing in
        //           its knowledge of the package list
        report = new PackageSyncReport();
        parser.parseResults(rssDocument1, report, newPackages);

        // Verify
        assert report.getNewPackages().size() == 0 : "New packages incorrectly reported. Expected: 0, Found: "
            + report.getNewPackages().size();
        assert report.getDeletedPackages().size() == 0 : "Incorrect number of deleted packages. Expected: 0, Found: "
            + report.getDeletedPackages().size();
        assert report.getUpdatedPackages().size() == 0 : "Incorrect number of updated packages. Expected: 0, Found: "
            + report.getUpdatedPackages().size();

        // Test #3 - Use second example set to trigger a deleted package
        report = new PackageSyncReport();
        parser.parseResults(rssDocument2, report, newPackages);

        assert report.getNewPackages().size() == 1 : "New packages incorrectly reported. Expected: 1, Found: "
            + report.getNewPackages().size();
        assert report.getDeletedPackages().size() == 1 : "Incorrect number of deleted packages. Expected: 0, Found: "
            + report.getDeletedPackages().size();
        assert report.getUpdatedPackages().size() == 0 : "Incorrect number of updated packages. Expected: 0, Found: "
            + report.getUpdatedPackages().size();
    }

    // Private  --------------------------------------------

    private RssDocument loadDocument(URL exampleFeedUrl) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(exampleFeedUrl.openStream());
        InputStreamReader isr = new InputStreamReader(bis);

        BufferedReader br = new BufferedReader(isr);

        StringBuffer rssFeedBuffer = new StringBuffer();
        String temp;
        while ((temp = br.readLine()) != null) {
            rssFeedBuffer.append(temp).append("\n");
        }

        br.close();

        return RssParser.parseRss(rssFeedBuffer.toString());
    }
}