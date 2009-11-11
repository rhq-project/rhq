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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.InitializationException;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSource;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.Certificate;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.CertificateFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.PublicKeyRing;


/**
 * @author pkilambi
 *
 */
public class RHNProvider implements ContentProvider, PackageSource, DistributionSource
{

    private final Log log = LogFactory.getLog(RHNProvider.class);
    private RHNActivator rhnObject;
    private RHNHelper helper;
    private boolean testMode = false;

    public void initializeForTest(Configuration configuration) throws Exception {
        log.info("**TEST MODE  RHNProvider is running in a test configuration**");
        testMode = true;
        initialize(configuration);
    }

    /**
     * Initializes the adapter with the specified configuration.
     *
     * <p/>Expects <u>one</u> of the following properties:
     *
     * <p/>
     * <table border="1">
     *   <tr>
     *     <td><b>location</b></td>
     *     <td>RHN Hosted server URL</td>
     *   </tr>
     *   <tr>
     *     <td><b>certificate</b></td>
     *     <td>A certificate for authentication and subscription validation.</td>
     *   </tr>
     * </table>
     *
     * <p/>
     *
     * @param  configuration The adapter's configuration propeties.
     *
     * @throws Exception On errors.
     */
    public void initialize(Configuration configuration) throws Exception {
        String locationIn = configuration.getSimpleValue("location", null);
        String certificate = configuration.getSimpleValue("certificate", null);

        String location = locationIn + RHNConstants.DEFAULT_HANDLER;

        location = trim(location);
        log.info("Initialized with location: " + location);
        certificate = certificate.trim();

        // check location field validity
        try {
            new URL(location);
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Invalid 'location' property");
        }

        // check certificate field validity
        try {
            Certificate cert = CertificateFactory.read(certificate);
            PublicKeyRing keyRing = this.readDefaultKeyRing();
            cert.verifySignature(keyRing);

        } catch (Exception e) {
            log.debug("Invalid Cert");
            throw new InitializationException("Invalid 'Certificate' property", e);
        }

        // Now we have valid data. Spawn the activation.
        try {
            rhnObject = new RHNActivator(certificate, location);
            rhnObject.processActivation();
            log.debug("Activation successful");
        } catch (Exception e) {
            log.debug("Activation Failed. Please check your configuration");
            throw new InitializationException("Server Activation Failed.", e);
        }

        String repos = configuration.getSimpleValue("SyncableChannels", null);
        log.info("Syncable Channel list :" + repos);

        // RHQ Server is now active, initialize the handler for the bits.
        helper = new RHNHelper(locationIn, repos, rhnObject.getSystemid());
        log.info("RHNProvider initialized RHNHelper with repos: " + repos);

        // Now that the server is activated, spawn all syncable channels
        ArrayList<String> channels = helper.getSyncableChannels();


        // when running under a test we are restricting behavior to not setup up Repos through RHQ
        if (!testMode) {
            // Eventually we should pass in a subset of selected channels to spawn
            initializeRepos(channels);
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
        return helper.openStream(location);
    }

    /**
     * Synchronize package content for selected channel labels
     */
    public void synchronizePackages(String repoName, PackageSyncReport report,
                                    Collection<ContentProviderPackageDetails> existingPackages)
        throws Exception {
        RHNSummary summary = new RHNSummary(helper);
        List<ContentProviderPackageDetails> deletedPackages = new ArrayList<ContentProviderPackageDetails>();
        deletedPackages.addAll(existingPackages);
        log.info("Report" + report);
        // sync now
        try {
            summary.markStarted();
            ArrayList pkgIds = helper.getChannelPackages();
            log.info("RHNProvider::  helper.getChannelPackages returned  " + pkgIds.size() + " packages");
            for (ContentProviderPackageDetails p : helper.getPackageDetails(pkgIds)) {
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
            throw e;
        } finally {
            //helper.disconnect();
            summary.markEnded();
            report.setSummary(summary.toString());
            log.info("synchronizing with repo: " + helper + " finished\n" + summary);
        }
    }

    public void synchronizeDistribution(DistributionSyncReport report, Collection<DistributionDetails> existingDistros) throws Exception {

        // Goal:
        //   This method will create the metadata representing what kickstart tree files need to be downloaded.
        //       the metadata will be returned through the DistributionSyncReport object.
        //   NOTE:  This method DOES not do the actual downloading of data.

        List<String> existingLabels = new ArrayList<String>();
        for (DistributionDetails d: existingDistros) {
            existingLabels.add(d.getLabel());
        }
        List<String> toSyncDistros = new ArrayList<String>();
        List<String> deletedDistros = new ArrayList<String>();  //Existing distros we want to remove.
        deletedDistros.addAll(existingLabels);

        List<String> availableLabels = helper.getChannelKickstartLabels();
        log.debug("Found " + availableLabels.size() + " available kickstart trees");
        for (String label: availableLabels) {
            log.debug("Processing kickstart: " + label);
            deletedDistros.remove(label);
            if (!existingLabels.contains(label)) {
                log.debug("New kickstart to sync: " + label);
                toSyncDistros.add(label);
            }
        }

        // Determine what distros are to be removed, i.e. they are synced by RHQ but no longer exist from RHN
        for (String label: deletedDistros) {
            for (DistributionDetails dd: existingDistros) {
                if (dd.getLabel().compareToIgnoreCase(label) == 0) {
                    report.addDeletedDistro(dd);
                }
            }
        }

        List<DistributionDetails> ddList = helper.getDistributionMetaData(toSyncDistros);
        report.addDistros(ddList);
    }

    /**
      * Test's the adapter's connection.
      *
      * @throws Exception When connection is not functional for any reason.
      */
    public void testConnection() throws Exception {
        rhnObject.processDeActivation();
        rhnObject.processActivation();
    }

    /**
     * Reads the public keyring on filesystem into memory
     *
     * @return A PublicKeyRing object.
     * @throws IOException On failing to read webapp keyring
     * @throws KeyException thrown on failing to validate the key 
     */
    private PublicKeyRing readDefaultKeyRing() throws KeyException, IOException {
        InputStream keyringStream = new FileInputStream(RHNConstants.DEFAULT_WEBAPP_GPG_KEY_RING);
        return new PublicKeyRing(keyringStream);
    }

    /**
     * Spawns a list of repos for all available channels from RHN hosted.
     * @param channels The list of channels to be initialized as repos
     * @throws RepoException
     */
    public void initializeRepos(ArrayList<String> channels) throws RepoException {
        log.info("list of channels: " + channels);
        if (channels.size() == 0) {
            // No repos to create
            return;
        }
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        Subject subject = LookupUtil.getSubjectManager().getOverlord();
        for (String clabel : channels) {
            Repo newRepo = new Repo(clabel.toString());
            newRepo = repoManager.createRepo(subject, newRepo);
            log.info("New " + newRepo + " repo created");
        }
    }

    /**
    * Trim white space and trailing (/) characters.
    *
    * @param  path A url/directory path string.
    *
    * @return A trimmed string.
    */
    private String trim(String path) {
        path = path.trim();
        while ((path.length() > 1) && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

}
