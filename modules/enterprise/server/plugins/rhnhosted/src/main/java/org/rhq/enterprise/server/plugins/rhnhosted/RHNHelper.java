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


import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnComm;
import org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.RhnDownloader;
import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetailsKey;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * @author pkilambi
 *
 */
public class RHNHelper {

    private final String baseurl;
    private final String repolabel;
    private final RhnComm rhndata;
    private final RhnDownloader rhndownload;
    private final String systemid;

    private final Log log = LogFactory.getLog(RHNHelper.class);

    /**
     * Constructor.
     *
     * @param baseurl The base url to connect to hosted
     * @param repolabelIn channel label
     */
    public RHNHelper(String baseurl, String repolabelIn) {

        this.baseurl = baseurl;
        this.repolabel = repolabelIn;
        this.rhndata = new RhnComm(baseurl);
        this.rhndownload = new RhnDownloader(baseurl);
        this.systemid = readSystemId();

    }

    /**
     * Extract the package metadata for all available packages to sync
     * @param packageIds Valid package ids for getPackageMatadata call to fetch from hosted
     * @return A list of package detail objects
     * @throws Exception On all errors
     */
    public List<ContentProviderPackageDetails> getPackageDetails(List packageIds)
            throws Exception {

        List<ContentProviderPackageDetails> pdlist = new ArrayList<ContentProviderPackageDetails>();
        List<RhnPackageType> pkgs = rhndata.getPackageMetadata(this.systemid, packageIds);
        for(RhnPackageType pkg: pkgs) {
            pdlist.add(getDetails(pkg));
        }
        return pdlist;
    }

    /**
     * Extract the package details for each rpm metadata fetched
     * 
     * @param p an rpm package metadata object
     *
     * @return ContentProviderPackageDetails pkg object
     */
    private ContentProviderPackageDetails getDetails(RhnPackageType p) {

        String name = p.getName();
        String version = p.getVersion();
        String arch = p.getPackageArch();
        String rpmname = constructRpmName(name, version, p.getRelease(), p.getEpoch(), arch);

        ContentProviderPackageDetailsKey key =
                new ContentProviderPackageDetailsKey(name, version, "rpm", arch, "Linux", "Platforms");
        ContentProviderPackageDetails pkg = new ContentProviderPackageDetails(key);

        pkg.setDisplayName(name);
        pkg.setShortDescription(p.getRhnPackageSummary());
        pkg.setLongDescription(p.getRhnPackageDescription());
        pkg.setFileName(rpmname);
        pkg.setFileSize(new Long(p.getPackageSize()));
        pkg.setFileCreatedDate(new Long(p.getLastModified()));
        pkg.setLicenseName("license");
        pkg.setMD5(p.getMd5Sum());
        pkg.setLocation(constructPackageUrl(repolabel, rpmname));
        //pkg.setMetadata();
        return pkg;
        
    }

    /**
     * Get List of packagesIds for Given Channels
     * @return List of all package ids associated to the channel
     * @throws IOException  on io errors on systemid reads
     * @throws XmlRpcException on xmlrpc faults
     */

    public ArrayList getChannelPackages() throws IOException, XmlRpcException {
        ArrayList<String> allPackages = new ArrayList();

        log.info("Systemid: " + this.systemid);
        log.info("repolist: " + this.repolabel);
        List<RhnChannelType> channels = rhndata.getChannels(this.systemid, Arrays.asList(this.repolabel));

        for (RhnChannelType channel : channels) {
            String packages = channel.getPackages();
            String[] pkgIds = packages.split(" ");
            if (pkgIds.length > 1) {
                allPackages.addAll(Arrays.asList(pkgIds));
            }
        }

        return allPackages;
    }

    /**
     * Get a list of all Syncable Channels based on entitled channel families
     * @return A list of channel labels
     * @throws IOException on systemid reads
     * @throws XmlRpcException on xmlrpc faults
     */
    public ArrayList getSyncableChannels() throws IOException, XmlRpcException {
        ArrayList<String> allchannels = new ArrayList();
        List<RhnChannelFamilyType> cfts = rhndata.getChannelFamilies(this.systemid);
        for (RhnChannelFamilyType cf : cfts) {
             String channeldata = cf.getChannelLabels();
             String[] clabels  = channeldata.split(" ");
             if (clabels.length > 1) {
                allchannels.addAll(Arrays.asList(clabels));
            }
        }

        return allchannels;
    }

    /**
     * Open an input stream to specifed relative url. Prepends the baseurl to the <i>url</i> and opens and opens and
     * input stream. Files with a .gz suffix will be unziped (inline).
     *
     * @param   location A url that is relative to the <i>baseurl</i> and references a file within the repo.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException  On io errors.
     *
     * @throws XmlRpcException On all errors.
     */
    public InputStream openStream(String location)
            throws IOException, XmlRpcException {

        log.info("Package Fetched from: " + location);
        return rhndownload.getRPMStream(this.systemid, location);
    }



    /**
     * Constructs a downloadable url for package downloads.
     * @param channelName channel label to be synced.
     * @param rpmName rpm file name
     * @return a valid url location to fetch the rpm from.
     */
    private String constructPackageUrl(String channelName, String rpmName) {

        String appendurl = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        return baseurl + appendurl;
    }

    /**
     * Method to construct an rpm format filename for download url
     * @param name  rpm package name
     * @param version  rpm package version
     * @param release  rpm package release
     * @param epoch   rpm package epoch
     * @param arch    rpm package arch
     * @return an rpm package name string
     */
    private String constructRpmName(String name, String version, String release, String epoch, String arch) {

        String releaseepoch = release + ":" + epoch;
        return name + "-" + version + "-" +  releaseepoch + "." + arch + ".rpm";
    }

    /**
     * Reads the systemId file from default location.
     * @return A systemId file content as a string
     */
    private String readSystemId() {

        try {
            File systemid_file = new File(RHNConstants.DEFAULT_SYSTEM_ID);
            return FileUtils.readFileToString(systemid_file);
        } catch (IOException e) {
            log.debug("SystemId file could not be read. Setting to empty string.");
            return "";
        }

    }

    /*
     * (non-Javadoc) @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return baseurl;
    }
}
