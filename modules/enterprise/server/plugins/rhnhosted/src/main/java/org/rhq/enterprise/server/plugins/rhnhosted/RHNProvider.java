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

 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.clientapi.server.plugin.content.ContentProvider;
 import org.rhq.core.clientapi.server.plugin.content.InitializationException;
 import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails;
 import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
 import org.rhq.core.clientapi.server.plugin.content.PackageSource;
 import org.rhq.enterprise.server.plugins.rhnhosted.certificate.Certificate;
 import org.rhq.enterprise.server.plugins.rhnhosted.certificate.CertificateFactory;
 import org.rhq.enterprise.server.plugins.rhnhosted.certificate.PublicKeyRing;

 import java.security.KeyException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.FileInputStream;
 import java.net.URL;
 import java.net.MalformedURLException;
 import java.util.Collection;
 import java.util.ArrayList;
 import java.util.List;


/**
 * @author pkilambi
 *
 */
public class RHNProvider implements ContentProvider, PackageSource {

    private final Log log = LogFactory.getLog(RHNProvider.class);
    private RHNConnector rhnObject;
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

        } catch(Exception e) {
            log.debug("Invalid Cert");
            throw new InitializationException("Invalid 'Certificate' property", e);
        }
        
        // Now we have valid data. Spawn the activation.
        try {
            rhnObject = new RHNConnector(certificate, location);
            rhnObject.processActivation();
            log.debug("Activation successful");
        } catch (Exception e) {
            log.debug("Activation Failed. Please check your configuration");
            throw new InitializationException("Server Activation Failed.", e);
        }

        String repos = configuration.getSimpleValue("SyncableChannels", null);
        log.info("Syncable Channel list :" + repos);


        // RHQ Server is now active, initialize the handler for the bits.
        helper = new RHNHelper(locationIn, repos);
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
    public void synchronizePackages(PackageSyncReport report, Collection<ContentProviderPackageDetails> existingPackages)
        throws Exception {
        RHNSummary summary = new RHNSummary(helper);
        List<ContentProviderPackageDetails> deletedPackages = new ArrayList<ContentProviderPackageDetails>();
        deletedPackages.addAll(existingPackages);
        log.info("Report" + report);
        
        // sync now
        try {
            summary.markStarted();
            ArrayList pkgIds = helper.getChannelPackages();
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
    private PublicKeyRing readDefaultKeyRing()
        throws KeyException, IOException {
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

}
