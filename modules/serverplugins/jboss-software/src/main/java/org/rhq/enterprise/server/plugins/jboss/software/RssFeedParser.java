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
package org.rhq.enterprise.server.plugins.jboss.software;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import churchillobjects.rss4j.RssChannel;
import churchillobjects.rss4j.RssChannelItem;
import churchillobjects.rss4j.RssDocument;
import churchillobjects.rss4j.RssDublinCore;
import churchillobjects.rss4j.RssJbnDependency;
import churchillobjects.rss4j.RssJbnPatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;

/**
 * Parses the contents of the JBoss RSS feed into the server's domain model.
 *
 * @author Jason Dobies
 */
public class RssFeedParser {
    // Constants  --------------------------------------------

    public static final String PLUGIN_NAME = "JBossAS";
    public static final String ARCHITECTURE = "noarch";

    /**
     * Corresponds to the resource type name defined in the JBoss AS plugin.
     */
    public static final String RESOURCE_TYPE_JBOSS_AS = "JBossAS Server";

    /**
     * Corresponds to the package type name defined in the JBoss AS plugin.
     */
    public static final String PACKAGE_TYPE_CUMULATIVE_PATCH = "cumulativePatch";

    public static final String RSS_SOFTWARE_TYPE_BUGFIX = "BUGFIX";
    public static final String RSS_SOFTWARE_TYPE_SECURITY = "SECURITY";
    public static final String RSS_SOFTWARE_TYPE_ENHANCEMENT = "ENHANCEMENT";
    public static final String RSS_SOFTWARE_TYPE_DISTRIBUTION = "DISTRIBUTION";

    public static final String DIST_STATUS_AVAILABLE = "AVAILABLE";
    public static final String DIST_STATUS_OBSOLETE = "OBSOLETE";
    public static final String DIST_STATUS_REMOVED = "REMOVED";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    // Public  --------------------------------------------

    public void parseResults(RssDocument feed, PackageSyncReport report,
        Collection<ContentSourcePackageDetails> existingPackages) {

        // Used to determine if a package was already sent to the server or is new
        Map<PackageDetailsKey, ContentSourcePackageDetails> existingPackageMap = unpack(existingPackages);

        Enumeration channels = feed.channels();

        while (channels.hasMoreElements()) {
            RssChannel channel = (RssChannel) channels.nextElement();
            Enumeration channeltems = channel.items();

            while (channeltems.hasMoreElements()) {
                RssChannelItem item = (RssChannelItem) channeltems.nextElement();

                RssJbnPatch patch = item.getJbnPatch();
                RssDublinCore dublinCore = item.getDublinCore();

                // We need the data in these objects, so skip if either are null. I'm not sure this constitutes
                // an error, but leaving the log message at warn for now.
                if ((dublinCore == null) || (patch == null)) {
                    log.debug("Feed entry parsed data returned null. Skipping entry.  Patch: " + patch
                        + ", Dublin Core: " + dublinCore);
                    continue;
                }

                // If there are no products against which the software applies, punch out early
                Collection products = patch.getProducts();
                if ((products == null) || (products.size() == 0)) {
                    continue;
                }

                // First class properties
                String packageName = dublinCore.getSubject();
                String softwareType = patch.getType();

                // Extra properties
                String distributionStatus = patch.getDistributionStatus();
                String jiraId = patch.getJira();
                String downloadUrl = patch.getDownloadUrl();
                String instructionCompatibilityVersion = patch.getInstructionCompatibilityVersion();

                // If the distribution status indicates it's removed, don't do anything. Later in this method, if
                // this package was known to the server, it will still be in the existing packages map and marked
                // as deleted at the end. If the server didn't know about it, then nothing had to be done.
                if (distributionStatus.equals(DIST_STATUS_REMOVED)) {
                    continue;
                }

                Configuration extraProperties = new Configuration();
                extraProperties.put(new PropertySimple("jiraId", jiraId));
                extraProperties.put(new PropertySimple("distributionStatus", distributionStatus));
                extraProperties.put(new PropertySimple("downloadUrl", downloadUrl));
                extraProperties.put(new PropertySimple("instructionCompatibilityVersion",
                    instructionCompatibilityVersion));

                /* This will be refactored when we add support for the other types of data coming across in the feed,
                   such as product distributions and security/configuration advisories. For now, just leaving the
                   cumulative patch handling in here, but will clean up when the other types are supported.
                   jdobies, Jan 9, 2008
                 */

                // Technically, this is a check for installable patches. But for now, that's the same as a cumulative
                // patch
                if (softwareType.equals(RSS_SOFTWARE_TYPE_BUGFIX) && instructionCompatibilityVersion != null) {
                    String version = parseCumulativePatchVersion(packageName);
                    if (version == null) {
                        log.error("Could not parse version for package: " + packageName);
                        continue;
                    }

                    ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(packageName, version,
                        PACKAGE_TYPE_CUMULATIVE_PATCH, ARCHITECTURE, RESOURCE_TYPE_JBOSS_AS, PLUGIN_NAME);

                    // If this package is already known to the server, don't add it as a new package
                    // Remove from the map; entries still in the map will be returned as deleted packages
                    if (existingPackageMap.get(key) != null) {
                        existingPackageMap.remove(key);
                        continue;
                    }

                    ContentSourcePackageDetails packageDetails = new ContentSourcePackageDetails(key);

                    packageDetails.setClassification(softwareType);
                    packageDetails.setDisplayName(packageName);
                    packageDetails.setFileCreatedDate(dublinCore.getDate().getTime());
                    packageDetails.setFileName(patch.getFileName());
                    packageDetails.setFileSize(Long.parseLong(patch.getFileSize()));
                    packageDetails.setLicenseName(patch.getLicenseName());
                    packageDetails.setLicenseVersion(patch.getLicenseVersion());
                    packageDetails.setLocation(patch.getAutomatedDownloadUrl());
                    packageDetails.setMD5(patch.getMd5());
                    packageDetails.setSHA265(patch.getSha256());

                    packageDetails.setShortDescription(patch.getShortDescription());
                    packageDetails.setLongDescription(patch.getLongDescription());

                    if (patch.getAutomatedInstallation() != null) {
                        String instructions = patch.getAutomatedInstallation();

                        // The instructions have XML fluff around the <process-definition> tag that is the actual
                        // process. There might be a cleaner way of doing this, but this should work
                        // JBNADM-3111
                        int processDefinitionStart = instructions.indexOf("<process-definition");
                        int processDefinitionEnd = instructions.indexOf("</process-definition>");

                        if (processDefinitionStart != -1 && processDefinitionEnd != -1) {
                            processDefinitionEnd += 22;

                            String choppedInstructions = instructions.substring(processDefinitionStart,
                                processDefinitionEnd);

                            packageDetails.setMetadata(choppedInstructions.getBytes());
                        }
                    }

                    packageDetails.setExtraProperties(extraProperties);

                    // For each product listed for the patch, add an entry for its resource version
                    for (Object productObj : products) {
                        RssJbnDependency product = (RssJbnDependency) productObj;

                        // The CSP feed will include an optional JON version that maps up to how the resource
                        // version will be identified by (now RHQ). If this is specified, we'll want to use
                        // the mapped version instead
                        String productVersion = product.getJonResourceVersion();

                        if (product.getProductVersion().equals("4.0.4")) {
                            productVersion = "4.0.4.GA";
                        }

                        if (productVersion == null) {
                            productVersion = product.getProductVersion();
                        }

                        packageDetails.addResourceVersion(productVersion);
                    }

                    report.addNewPackage(packageDetails);
                }
            }
        }

        // For each entry still in the map, we didn't find it again, so report it as deleted
        for (ContentSourcePackageDetails pkg : existingPackageMap.values()) {
            report.addDeletePackage(pkg);
        }
    }

    // Private  --------------------------------------------

    /**
     * Translates the set of packages into a map, using the package key object as the map's key entry.
     *
     * @param  existingPackages packages sent from the server as already known for this content source
     *
     * @return map of the same size as the existingPackages collection; empty map if existingPackages is <code>
     *         null</code>
     */
    private Map<PackageDetailsKey, ContentSourcePackageDetails> unpack(
        Collection<ContentSourcePackageDetails> existingPackages) {
        Map<PackageDetailsKey, ContentSourcePackageDetails> map = new HashMap<PackageDetailsKey, ContentSourcePackageDetails>();

        if (existingPackages == null) {
            return map;
        }

        for (ContentSourcePackageDetails pkg : existingPackages) {
            map.put(pkg.getKey(), pkg);
        }

        return map;
    }

    /**
     * Parses out the cumulative patch version from the package title.
     *
     * @param  title name of the package
     *
     * @return patch string if the title matches the expected title for a cumulative patch; <code>null</code> otherwise
     */
    private String parseCumulativePatchVersion(String title) {
        if (title.startsWith("JBoss AS ")) {
            return title.substring(9);
        } else if (title.startsWith("JBoss EAP ")) {
            return title.substring(10);
        } else if (title.startsWith("JBoss SOA ")) {
            return title.substring(10);
        }

        return null;
    }
}