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
package org.rhq.enterprise.server.plugins.yum;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.SyncException;
import org.rhq.enterprise.server.plugin.pc.content.SyncProgressWeight;

/**
 * The RepoSource provides a content source for synchronizing content contained with a yum repo.
 *
 * @author jortel
 */
public class RepoProvider implements ContentProvider, PackageSource {
    /**
     * The reader used to access a yum repo's metadata and packages.
     */
    private RepoReader reader;

    /**
     * A repo object used to access the specifed yum repo's metadata and packages.
     */
    private Repo repo;

    /**
     * Logger
     */
    private final Log log = LogFactory.getLog(RepoProvider.class);

    /**
     * Initializes the adapter with the specified configuration.
     *
     * <p/>Expects <u>one</u> of the following properties:
     *
     * <p/>
     * <table border="1">
     *   <tr>
     *     <td><b>path</b></td>
     *     <td>A file system root directory or mount point</td>
     *   </tr>
     *   <tr>
     *     <td><b>url</b></td>
     *     <td>A <i>base</i> URL for a yum repo</td>
     *   </tr>
     * </table>
     *
     * <p/>Constructs the appropriate repo reader based on with of these parameters are specified.
     *
     * @param  configuration The adapter's configuration propeties.
     *
     * @throws Exception On errors.
     */
    public void initialize(Configuration configuration) throws Exception {
        String location = configuration.getSimpleValue("location", null);
        if (location == null) {
            throw new IllegalArgumentException("Missing required 'location' property");
        }

        location = location.trim();
        String username = configuration.getSimpleValue("username");
        String password = configuration.getSimpleValue("password");

        URI uri = new URI(location);

        log.info("Initialized with location: " + location);
        try {
            reader = UrlReader.fromUri(uri, username, password);
        } catch (MalformedURLException e) {
            log.error("Could not determine a reader for the URI [" + uri + "]");
            throw e;
        }
    }

    /**
     * Shutdown the adapter.
     */
    public void shutdown() {
        log.debug("shutdown");
    }

    /**
     * Get an input stream for the specified package (bits).
     *
     * @param  location The location relative to the baseurl.
     *
     * @return An open stream that <b>must</b> be closed by the caller.
     *
     * @throws Exception On all errors.
     */
    public InputStream getInputStream(String location) throws Exception {
        log.debug("opening: " + location);
        return reader.openStream(location);
    }

    /**
     * Synchronizes the packages contained within the yum repo. Reads the repo's metadata and updates the report to
     * indicate packages that need to be added and deleted. The notion of updated packages does not make sense in the
     * rpm works since an update generates a new package version.
     *
     * @param repoName
     *@param  report           A report to fill in.
     * @param  existingPackages A collection of package specifications already in inventory.
    *   @throws Exception On all errors.
     */
    public void synchronizePackages(String repoName, PackageSyncReport report,
        Collection<ContentProviderPackageDetails> existingPackages) throws SyncException, InterruptedException {
        Summary summary = new Summary(reader);
        log.info("synchronizing with repository: " + reader + " started");
        try {
            summary.markStarted();
            repo = new Repo(reader);
            repo.connect();
            List<ContentProviderPackageDetails> deletedPackages = new ArrayList<ContentProviderPackageDetails>();
            deletedPackages.addAll(existingPackages);
            for (ContentProviderPackageDetails p : repo.getPackageDetails()) {
                log.debug("Processing package at (" + p.getLocation());
                deletedPackages.remove(p);
                if (!existingPackages.contains(p)) {
                    log.debug("New package at (" + p.getLocation() + ") detected");
                    report.addNewPackage(p);
                    summary.added++;
                }
            }

            for (ContentProviderPackageDetails p : deletedPackages) {
                log.debug("Package at (" + p.getDisplayName() + ") marked as deleted");
                report.addDeletePackage(p);
                summary.deleted++;
            }
        } catch (Exception e) {
            summary.errors.add(e.toString());
            throw new SyncException("error synching synchronizePackages", e);
        } finally {
            repo.disconnect();
            summary.markEnded();
            report.setSummary(summary.toString());
            log.info("synchronizing with repository: " + reader + " finished\n" + summary);
        }
    }

    /**
     * Test's the adapter's connection.
     *
     * @throws Exception When connection is not functional for any reason.
     */
    public void testConnection() throws Exception {
        reader.validate();
    }

    public SyncProgressWeight getSyncProgressWeight() {
        return SyncProgressWeight.DEFAULT_WEIGHTS;
    }
}
