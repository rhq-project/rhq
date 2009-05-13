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
package org.rhq.enterprise.server.plugins.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;

/**
 * This is a basic implementation of a content source that provides primative package
 * synchronization with a URL-based content source, such as an HTTP server.
 * 
 * In order for this URL content source to properly scan and find content in the
 * remote server, an index file must exist that provides a list of
 * each file relative to the root URL location. This index file can include paths to subdirectories
 * under the root URL. The index file must be named "content-index.txt" unless
 * overridden by the content source's configuration setting.
 * 
 * The metadata stored in this index file describes the packages.
 * Each line in the index file must be a single filename, followed by the MD5 of the files:
 *
 * <pre>
 * release-v1.0.zip|abe347586edbc6723461253457687bef
 * release-v2.0.zip|456834fed6edb3452346125345768723d
 * patches/patch-123.jar|56bc47586e5456edfb6a2534e7687345
 * patches/patch-4567.jar|dcb567886eabc6723461253457687bef
 * </pre>
 *
 * Subclasses can override this class if they want to support more full-featured metadata
 * (for example, an RSS feed found at the index URL).
 *
 * The index file can be specified as a full URL - if it is not, it will be assumed relative to
 * the root URL.
 * 
 * Note that this is a very inefficient type of content source because of the lack of metadata.
 * Because the only thing we know is the URL to a piece of content and nothing else, the only way
 * to determine things like version number is to possible download the content to scan it.
 *
 * @author John Mazzitelli
 */
public class UrlSource implements ContentSourceAdapter {

    /**
     * The default index file is located at the root URL under this filename.
     */
    private static final String DEFAULT_INDEX_FILENAME = "content-index.txt";

    /**
     * The root URL from which to synchronize content.
     */
    private URL rootUrl;

    /**
     * Just a stringified version of root URL that is used to build a full URL to content.
     * This will be ensured to end with a "/".
     */
    private String rootUrlString;

    /**
     * The URL to the file that contains the list of all content found in the remote server.
     */
    private URL indexUrl;

    /**
     * Map of all supported package types keyed on filename filter regex's that define
     * which files match to which package types.
     */
    private Map<String, SupportedPackageType> supportedPackageTypes;

    protected URL getRootUrl() {
        return this.rootUrl;
    }

    protected void setRootUrl(URL url) {
        this.rootUrl = url;
        this.rootUrlString = url.toString();
        if (!this.rootUrlString.endsWith("/")) {
            this.rootUrlString = this.rootUrlString + "/";
        }
    }

    protected URL getIndexUrl() {
        return this.indexUrl;
    }

    protected void setIndexUrl(URL indexURL) {
        this.indexUrl = indexURL;
    }

    protected Map<String, SupportedPackageType> getSupportedPackageTypes() {
        return this.supportedPackageTypes;
    }

    protected void setSupportedPackageTypes(Map<String, SupportedPackageType> supportedPackageTypes) {
        this.supportedPackageTypes = supportedPackageTypes;
    }

    public void initialize(Configuration configuration) throws Exception {
        initializePackageTypes(configuration);

        String rootUrlString = configuration.getSimpleValue("rootUrl", null);
        String indexFileString = configuration.getSimpleValue("indexFile", DEFAULT_INDEX_FILENAME);

        // if the index file has the character ":" in it, it is assumed to be a full URL.
        // otherwise, it is assumed to be a file relative to the root URL.
        if (indexFileString.indexOf(":") > -1) {
            setIndexUrl(new URL(indexFileString));
        } else {
            if (!rootUrlString.endsWith("/") && !indexFileString.startsWith("/")) {
                indexFileString = "/" + indexFileString;
            }
            setIndexUrl(new URL(rootUrlString + indexFileString));
        }

        URI uri = new URI(rootUrlString);
        URL url = uri.toURL(); // proper RFC2396 decode happens here
        setRootUrl(url);

        testConnection();
    }

    public void shutdown() {
        this.rootUrl = null;
        this.rootUrlString = null;
        this.indexUrl = null;
        this.supportedPackageTypes = null;
    }

    public void synchronizePackages(PackageSyncReport report, Collection<ContentSourcePackageDetails> existingPackages)
        throws Exception {

        // put all existing packages in a "to be deleted" list. As we sync, we will remove
        // packages from this list that still exist on the remote system. Any leftover in the list
        // are packages that no longer exist on the remote system and should be removed from the server inventory.
        List<ContentSourcePackageDetails> deletedPackages = new ArrayList<ContentSourcePackageDetails>();
        deletedPackages.addAll(existingPackages);

        // sync now
        long before = System.currentTimeMillis();

        Map<String, RemotePackageInfo> locationList = getRemotePackageInfosFromIndex();
        for (RemotePackageInfo rpi : locationList.values()) {
            syncPackage(report, deletedPackages, rpi);
        }

        long elapsed = System.currentTimeMillis() - before;

        // if there are packages that weren't found on the remote system, tell server to remove them from inventory
        for (ContentSourcePackageDetails p : deletedPackages) {
            report.addDeletePackage(p);
        }

        report.setSummary("Synchronized [" + getRootUrl() + "]. Elapsed time=[" + elapsed + "] ms");

        return;
    }

    public void testConnection() throws Exception {
        // to test, just make sure we can read the index file;
        // errors will caused exceptions to be thrown.
        URL url = getIndexUrl();
        URLConnection urlConn = url.openConnection();
        urlConn.getContentLength();
        return;
    }

    public InputStream getInputStream(String location) throws Exception {
        URL locationUrl = new URL(this.rootUrlString + location);
        return locationUrl.openStream();
    }

    /**
     * Returns the stream that contains the {@link #getIndexUrl() index file} content.
     * @return index file content stream
     * @throws Exception
     */
    protected InputStream getIndexInputStream() throws Exception {
        InputStream indexStream = getIndexUrl().openStream();
        return indexStream;
    }

    /**
     * Returns info on all the files listed in the {@link #getIndexUrl() index file}.
     * Blank lines in the index file will be ignored.
     * 
     * @return map of Strings/Infos, where each string is the location relative to the root URL. Each string
     *         in the map keys is guaranteed not to have a leading slash.
     *         
     * @throws Exception if the index file is missing or cannot be processed
     */
    protected Map<String, RemotePackageInfo> getRemotePackageInfosFromIndex() throws Exception {
        Map<String, RemotePackageInfo> fileList = new HashMap<String, RemotePackageInfo>();

        InputStream indexStream = getIndexInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                while (line.startsWith("/")) {
                    line = (line.length() > 1) ? line.substring(1) : ""; // we don't want a leading slash
                }
                if (line.length() > 0) {
                    String[] locationAndMd5 = line.split("\\|");
                    // if the line doesn't have name|md5, we silently skip it
                    if (locationAndMd5.length == 2) {
                        URL locationUrl = new URL(this.rootUrlString + locationAndMd5[0]);
                        RemotePackageInfo rpi = new RemotePackageInfo(locationAndMd5[0], locationUrl, locationAndMd5[1]);
                        fileList.put(rpi.getLocation(), rpi);
                    }
                }
            }
        } finally {
            indexStream.close();
        }

        return fileList;
    }

    /**
     * Builds up the report of packages by adding to it the content that is being processed.
     * As content is found, their associated packages are removed from <code>packages</code> if they exist
     * leaving only packages remaining that do not exist on the remote system.
     * 
     * @param report the report that we are building up
     * @param packages existing packages not yet found on the remote system but exist in server inventory
     * @param rpi information about the package that needs to be synced
     * 
     * @throws Exception if the sync fails
     */
    protected void syncPackage(PackageSyncReport report, List<ContentSourcePackageDetails> packages,
        RemotePackageInfo rpi) throws Exception {

        ContentSourcePackageDetails details = createPackage(rpi);
        if (details != null) {
            ContentSourcePackageDetails existing = findPackage(packages, details);
            if (existing == null) {
                report.addNewPackage(details);
            } else {
                packages.remove(existing); // it still exists, remove it from our list
                if (details.getFileCreatedDate().compareTo(existing.getFileCreatedDate()) > 0) {
                    report.addUpdatedPackage(details);
                }
            }
        } else {
            // file does not match any filter and is therefore an unknown type - ignore it
        }

        return;
    }

    /**
     * Created the package details given the remote package information.
     *
     * @param rpi information about the remote package
     * @return the full details about the package
     *
     * @throws Exception
     */
    protected ContentSourcePackageDetails createPackage(RemotePackageInfo rpi) throws Exception {

        SupportedPackageType supportedPackageType = determinePackageType(rpi);
        if (supportedPackageType == null) {
            return null; // we can't handle this file - it is an unknown/unsupported package type
        }

        String md5 = rpi.getMD5();
        String name = new File(rpi.getLocation()).getName();
        String version = md5; // very crude, but our 
        String packageTypeName = supportedPackageType.packageTypeName;
        String architectureName = supportedPackageType.architectureName;
        String resourceTypeName = supportedPackageType.resourceTypeName;
        String resourceTypePluginName = supportedPackageType.resourceTypePluginName;

        ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(name, version, packageTypeName,
            architectureName, resourceTypeName, resourceTypePluginName);
        ContentSourcePackageDetails pkg = new ContentSourcePackageDetails(key);

        URLConnection urlConn = rpi.getUrl().openConnection();
        pkg.setFileCreatedDate(urlConn.getLastModified());
        pkg.setFileSize(new Long(urlConn.getContentLength()));
        pkg.setDisplayName(name);
        pkg.setFileName(name);
        pkg.setMD5(md5);
        pkg.setLocation(rpi.getLocation());
        pkg.setShortDescription(null);

        return pkg;
    }

    protected ContentSourcePackageDetails findPackage(List<ContentSourcePackageDetails> packages,
        ContentSourcePackageDetails pkg) {
        for (ContentSourcePackageDetails p : packages) {
            if (p.equals(pkg)) {
                return p;
            }
        }
        return null;
    }

    protected void initializePackageTypes(Configuration config) {

        Map<String, SupportedPackageType> supportedPackageTypes = new HashMap<String, SupportedPackageType>();

        /* TODO: THE UI CURRENTLY DOES NOT SUPPORT ADDING/EDITING LIST OF MAPS, SO WE CAN ONLY SUPPORT ONE PACKAGE TYPE 
         * SO FOR NOW, JUST SUPPORT A FLAT SET OF SIMPLE PROPS - WE WILL RE-ENABLE THIS CODE LATER */
        if (false) {
            // All of these properties must exist, any nulls should trigger runtime exceptions which is what we want
            // because if the configuration is bad, this content source should not initialize. 
            List<Property> packageTypesList = config.getList("packageTypes").getList();
            for (Property property : packageTypesList) {
                PropertyMap pkgType = (PropertyMap) property;
                SupportedPackageType supportedPackageType = new SupportedPackageType();
                supportedPackageType.packageTypeName = pkgType.getSimpleValue("packageTypeName", null);
                supportedPackageType.architectureName = pkgType.getSimpleValue("architectureName", null);
                supportedPackageType.resourceTypeName = pkgType.getSimpleValue("resourceTypeName", null);
                supportedPackageType.resourceTypePluginName = pkgType.getSimpleValue("resourceTypePluginName", null);

                String filenameFilter = pkgType.getSimpleValue("filenameFilter", null);
                supportedPackageTypes.put(filenameFilter, supportedPackageType);
            }
        } else {
            /* THIS CODE IS THE FLAT SET OF PROPS - USE UNTIL WE CAN EDIT MAPS AT WHICH TIME DELETE THIS ELSE CLAUSE */
            SupportedPackageType supportedPackageType = new SupportedPackageType();
            supportedPackageType.packageTypeName = config.getSimpleValue("packageTypeName", null);
            supportedPackageType.architectureName = config.getSimpleValue("architectureName", null);
            supportedPackageType.resourceTypeName = config.getSimpleValue("resourceTypeName", null);
            supportedPackageType.resourceTypePluginName = config.getSimpleValue("resourceTypePluginName", null);
            String filenameFilter = config.getSimpleValue("filenameFilter", null);
            supportedPackageTypes.put(filenameFilter, supportedPackageType);
        }

        setSupportedPackageTypes(supportedPackageTypes);
        return;
    }

    protected SupportedPackageType determinePackageType(RemotePackageInfo rpi) {
        for (Map.Entry<String, SupportedPackageType> entry : getSupportedPackageTypes().entrySet()) {
            if (rpi.getLocation().matches(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null; // the file doesn't match any known types for this content source
    }

    protected class SupportedPackageType {
        public String packageTypeName;
        public String architectureName;
        public String resourceTypeName;
        public String resourceTypePluginName;
    }
}