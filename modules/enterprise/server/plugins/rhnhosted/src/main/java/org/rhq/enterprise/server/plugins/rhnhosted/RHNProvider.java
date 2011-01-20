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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.enterprise.server.plugin.pc.content.AdvisoryDetails;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySource;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySyncReport;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionDetails;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSource;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.InitializationException;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.RepoDetails;
import org.rhq.enterprise.server.plugin.pc.content.RepoImportReport;
import org.rhq.enterprise.server.plugin.pc.content.RepoSource;
import org.rhq.enterprise.server.plugin.pc.content.SyncException;
import org.rhq.enterprise.server.plugin.pc.content.SyncProgressWeight;
import org.rhq.enterprise.server.plugin.pc.content.ThreadUtil;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.Certificate;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.CertificateFactory;
import org.rhq.enterprise.server.plugins.rhnhosted.certificate.PublicKeyRing;

/**
 * @author pkilambi
 *
 */
public class RHNProvider implements ContentProvider, PackageSource, RepoSource, DistributionSource, AdvisorySource {

    private final Log log = LogFactory.getLog(RHNProvider.class);
    private RHNActivator rhnObject;
    private RHNHelper helper;

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

        String systemId = FileUtils.readFileToString(new File(RHNConstants.DEFAULT_SYSTEM_ID));
        helper = new RHNHelper(locationIn, systemId);
        log.info("RHNProvider initialized RHNHelper with location = " + locationIn + "\n " + "systemid = " + systemId);
        // Check basic authentication capabilities with passed in systemid and RHN server
        // If there is a problem a XmlRpcException will be thrown, we'll allow it to be thrown and not try to catch it
        helper.checkSystemId(systemId);

        // Now we have valid data. Spawn the activation.
        try {
            rhnObject = new RHNActivator(systemId, certificate, location);
            rhnObject.processActivation();
            log.debug("Activation successful");
        } catch (Exception e) {
            log.debug("Activation Failed. Please check your configuration");
            throw new InitializationException("Server Activation Failed.", e);
        }
    }

    /**
     * @inheritDoc
     */
    public void shutdown() {
        log.debug("shutdown");
    }

    /**
     * @inheritDoc
     */
    public InputStream getInputStream(String location) throws Exception {
        log.debug("opening: " + location);
        return helper.openStream(location);
    }

    /**
     * @inheritDoc
     */
    public void synchronizePackages(String repoName, PackageSyncReport report,
        Collection<ContentProviderPackageDetails> existingPackages) throws SyncException, InterruptedException {
        log.info("synchronizePackages(repoName = " + repoName + ", report = " + report + ", existingPackages.size() = "
            + existingPackages.size());
        RHNSummary summary = new RHNSummary(helper);
        List<ContentProviderPackageDetails> deletedPackages = new ArrayList<ContentProviderPackageDetails>();
        deletedPackages.addAll(existingPackages);
        log.info("Report" + report);
        // sync now
        try {
            summary.markStarted();
            List<String> pkgIds = helper.getChannelPackages(repoName);
            log.info("RHNProvider::  helper.getChannelPackages returned  " + pkgIds.size() + " packages");
            List<ContentProviderPackageDetails> pkgDetails = new ArrayList<ContentProviderPackageDetails>();

            //
            // We ran into problems when syncing large package lists, example 6000 packages.
            // We are going to chunk the list to processing a smaller amount at a time.
            //
            long startTime = System.currentTimeMillis();
            int sliceSize = 100;
            for (int index = 0; index < pkgIds.size(); index += sliceSize) {
                int end = index + sliceSize;
                if (end >= pkgIds.size()) {
                    end = pkgIds.size() - 1;
                }
                long startTimeSlice = System.currentTimeMillis();
                log.debug("Getting package details for slice [" + index + " -> " + end + "]");
                List<String> pkgSliceList = pkgIds.subList(index, end);
                List<ContentProviderPackageDetails> tempList = helper.getPackageDetails(pkgSliceList, repoName);
                log.debug("We called getPackageDetails() on a list of " + pkgSliceList.size()
                    + " pkg ids and got a return list of " + tempList.size() + " packages");
                pkgDetails.addAll(tempList);
                long endTimeSlice = System.currentTimeMillis();
                log.debug("Slice processed in " + (endTimeSlice - startTimeSlice) + "ms current size of pkgDetails is "
                    + pkgDetails.size());
                ThreadUtil.checkInterrupted();
            }
            long endTime = System.currentTimeMillis();
            log.info("It took " + (endTime - startTime) + "ms too get PackageDetails for " + pkgIds.size()
                + " packages");
            log.info("We fetched metadata for " + pkgDetails.size() + " packages, passed in list of pkgIds was: "
                + pkgIds.size());

            for (ContentProviderPackageDetails p : pkgDetails) {
                log.debug("Processing package at (" + p.getLocation());
                deletedPackages.remove(p);
                if (!existingPackages.contains(p)) {
                    log.debug("New package at (" + p.getLocation() + ") detected");
                    report.addNewPackage(p);
                    summary.added++;
                }
                ThreadUtil.checkInterrupted();
            }
            for (ContentProviderPackageDetails p : deletedPackages) {
                log.debug("Package at (" + p.getDisplayName() + ") marked as deleted");
                report.addDeletePackage(p);
                summary.deleted++;
            }
        } catch (Exception e) {
            summary.errors.add(e.toString());
            throw new SyncException("error synching packages.", e);
        } finally {
            //helper.disconnect();
            summary.markEnded();
            report.setSummary(summary.toString());
            log.info("synchronizing with repository: " + helper + " finished\n" + summary);
        }
    }

    /**
     * @return null so that the {@link PackageVersion#DEFAULT_COMPARATOR} is used because it is well suited for the RHN packages.
     */
    public Comparator<PackageVersion> getPackageVersionComparator() {
        return null;
    }
    
    /**
     * @inheritDoc
     */
    public void synchronizeAdvisory(String repoName, AdvisorySyncReport report,
        Collection<AdvisoryDetails> existingAdvisory) throws SyncException, InterruptedException {

        List<String> existingLabels = new ArrayList<String>();
        for (AdvisoryDetails ad : existingAdvisory) {
            existingLabels.add(ad.getAdvisory());
        }
        List<String> toSyncAdvs = new ArrayList<String>();
        List<String> deletedAdvs = new ArrayList<String>(); //Existing advisories we want to remove.
        deletedAdvs.addAll(existingLabels);

        try {
            List<String> errataIds = helper.getChannelAdvisory(repoName);
            List<AdvisoryDetails> advList = helper.getAdvisoryMetadata(errataIds, repoName);
            log.debug("Found " + advList.size() + " available errata");
            for (AdvisoryDetails adv : advList) {
                log.debug("Processing Advisory ::" + adv.getAdvisory());
                deletedAdvs.remove(adv.getAdvisory());
                if (!existingLabels.contains(adv.getAdvisory())) {
                    log.debug("New Advisory " + adv.getAdvisory() + ") detected" + " with bugs" + adv.getBugs()
                        + "with cves" + adv.getCVEs() + "wiuth packages" + adv.getPkgs());
                    report.addAdvisory(adv);
                }
                ThreadUtil.checkInterrupted();
            }
        } catch (IOException ioe) {
            throw new SyncException("IOException syncing advisory meta", ioe);
        } catch (XmlRpcException x) {
            throw new SyncException("XmlRpcException syncing advisory meta", x);
        }

        for (String adv : deletedAdvs) {
            for (AdvisoryDetails advd : existingAdvisory) {
                if (advd.getAdvisory().compareToIgnoreCase(adv) == 0) {
                    report.addDeletedAdvisory(advd);
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void synchronizeDistribution(String repoName, DistributionSyncReport report,
        Collection<DistributionDetails> existingDistros) throws SyncException {

        // Goal:
        //   This method will create the metadata representing what kickstart tree files need to be downloaded.
        //       the metadata will be returned through the DistributionSyncReport object.
        //   NOTE:  This method DOES not do the actual downloading of data.

        List<String> existingLabels = new ArrayList<String>();
        for (DistributionDetails d : existingDistros) {
            existingLabels.add(d.getLabel());
        }
        List<String> toSyncDistros = new ArrayList<String>();
        List<String> deletedDistros = new ArrayList<String>(); //Existing distros we want to remove.
        deletedDistros.addAll(existingLabels);

        List<String> availableLabels;
        try {
            availableLabels = helper.getSyncableKickstartLabels(repoName);
        } catch (Exception e) {
            throw new SyncException("Error synching kickstart labels", e);
        }
        log.debug("Found " + availableLabels.size() + " available kickstart trees");
        for (String label : availableLabels) {
            log.debug("Processing kickstart: " + label);
            deletedDistros.remove(label);
            if (!existingLabels.contains(label)) {
                log.debug("New kickstart to sync: " + label);
                toSyncDistros.add(label);
            }
        }

        // Determine what distros are to be removed, i.e. they are synced by RHQ but no longer exist from RHN
        for (String label : deletedDistros) {
            for (DistributionDetails dd : existingDistros) {
                if (dd.getLabel().compareToIgnoreCase(label) == 0) {
                    report.addDeletedDistro(dd);
                }
            }
        }

        List<DistributionDetails> ddList;
        try {
            ddList = helper.getDistributionMetaData(toSyncDistros);
        } catch (Exception e) {
            throw new SyncException("Error synching distro metadata", e);
        }
        report.addDistros(ddList);
    }

    /**
     * @inheritDoc
     */
    public void testConnection() throws Exception {
        rhnObject.processDeActivation();
        rhnObject.processActivation();
    }

    /**
     * @inheritDoc
     */
    public RepoImportReport importRepos() throws Exception {
        RepoImportReport report = new RepoImportReport();

        List<String> channels = helper.getSyncableChannels();
        for (String clabel : channels) {
            log.info("Importing repo: " + clabel);
            RepoDetails repo = new RepoDetails(clabel);
            report.addRepo(repo);
        }

        return report;
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

    /**
     * @inheritDoc
     */
    public String getDistFileRemoteLocation(String repoName, String label, String relativeFilename) {
        return helper.constructKickstartFileUrl(repoName, label, relativeFilename);
    }

    public SyncProgressWeight getSyncProgressWeight() {
        return new SyncProgressWeight(10, 1, 0, 0, 1);
    }

}
