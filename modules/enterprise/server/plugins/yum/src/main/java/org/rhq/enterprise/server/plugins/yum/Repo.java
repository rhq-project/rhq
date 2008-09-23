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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.domain.content.PackageDetails;
import org.rhq.enterprise.server.plugins.yum.Repodata.Meta;

/**
 * The Repo is a proxy for a local or remote yum repository.
 *
 * @author jortel
 */
public class Repo {
    /**
     * A repodata object used to access the yum repo's content.
     */
    private Repodata repodata;

    /**
     * Constructor.
     *
     * @param reader An initialized repo reader.
     */
    public Repo(RepoReader reader) {
        repodata = new Repodata(reader);
    }

    /**
     * Connect to the yum repo.
     *
     * @throws Exception On connection error.
     */
    public void connect() throws Exception {
        repodata.refresh();
    }

    /**
     * Disconnect and reset object.
     */
    public void disconnect() {
        repodata.clear();
    }

    /**
     * Get a string representation of metadata entry contained within the primary.xml for the specified package by name.
     *
     * @param  pkgname A fully qualified package name.
     *
     * @return A <i>string</i> XML fragment when found, else null.
     *
     * @throws Exception On all errors.
     */
    public String getPackageMetadataString(String pkgname) throws Exception {
        return Repodata.toString(getPackageMetadata(pkgname));
    }

    /**
     * Get the metadata entry contained within the primary.xml for the specified package by name.
     *
     * @param  pkgname A fully qualified package name.
     *
     * @return A node containing the packages metadata when found, else null.
     *
     * @throws Exception On all errors.
     */
    @SuppressWarnings("unchecked")
    public Element getPackageMetadata(String pkgname) throws Exception {
        Element primary = repodata.getMetadata(Meta.primary);
        for (Element p : (List<Element>) primary.getChildren("package", Repodata.yumns)) {
            if (p.getChildText("name", Repodata.yumns).equals(pkgname)) {
                return p;
            }
        }

        return null;
    }

    /**
     * Get a list of {@link org.rhq.core.domain.content.PackageDetails } for all packages contained with the yum repo.
     *
     * @return A list of package details.
     *
     * @throws Exception On all errors.
     */
    @SuppressWarnings("unchecked")
    public List<ContentSourcePackageDetails> getPackageDetails() throws Exception {
        List<ContentSourcePackageDetails> list = new ArrayList<ContentSourcePackageDetails>();
        Element primary = repodata.getMetadata(Meta.primary);
        for (Element p : (List<Element>) primary.getChildren("package", Repodata.yumns)) {
            list.add(getDetails(p));
        }

        return list;
    }

    /**
     * Get a {@link org.rhq.core.domain.content.PackageDetails } for the specified package by name.
     *
     * @param  pkgname A fully qualified package name.
     *
     * @return The found package detail when found, else null.
     *
     * @throws Exception On all errors.
     */
    public PackageDetails getPackageDetails(String pkgname) throws Exception {
        return getDetails(getPackageMetadata(pkgname));
    }

    /**
     * Open and return an input stream for the specified package (bit) by name.
     *
     * @param  pkgname A fully qualified package name.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws Exception On all erorrs.
     */
    public InputStream openPackageStream(String pkgname) throws Exception {
        String location = packageLocation(pkgname);
        return repodata.getReader().openStream(location);
    }

    /**
     * Get the location (href) of the specified package.
     *
     * @param  pkgname A fully qualified package name.
     *
     * @return The <i>href</i> of the specified package if found, else null.
     *
     * @throws Exception On all errors.
     */
    public String packageLocation(String pkgname) throws Exception {
        Element p = getPackageMetadata(pkgname);
        return p.getChild("location", Repodata.yumns).getAttributeValue("href");
    }

    /**
     * Encode the <i>version</i> element into an internal version representation. This representation is the
     * {epoch}.{ver}.{rel} formatted string.
     *
     * @param  version A primary.xml version node.
     *
     * @return An encoded string representation of the rpm's version.
     */
    private String encodeVersion(Element p) {
        Element version = p.getChild("version", Repodata.yumns);
        StringBuilder sb = new StringBuilder();
        sb.append(version.getAttributeValue("epoch"));
        sb.append('.');
        sb.append(version.getAttributeValue("ver"));
        sb.append('.');
        sb.append(version.getAttributeValue("rel"));
        return sb.toString();
    }

    /**
     * Get the filename from the specified package node.
     *
     * @param  p A (primary.xml) package node.
     *
     * @return The package file name.
     */
    private String filename(Element p) {
        String[] parts = location(p).split("[/\\\\]");
        return parts[parts.length - 1];
    }

    /**
     * Get the package file date.
     *
     * @param  p A (primary.xml) package node.
     *
     * @return The package's file date.
     */
    private Long filedate(Element p) {
        String date = p.getChild("time", Repodata.yumns).getAttributeValue("file");
        return Long.parseLong(date);
    }

    private String location(Element p) {
        return p.getChild("location", Repodata.yumns).getAttributeValue("href");
    }

    /**
     * Create and return a {@link org.rhq.core.domain.content.PackageDetails } object of rthe specified (primary.xml)
     * package node.
     *
     * @param  p A primary.xml package node.
     *
     * @return A {@link org.rhq.core.domain.content.PackageDetails } object.
     */
    private ContentSourcePackageDetails getDetails(Element p) throws Exception {
        String name = p.getChildText("name", Repodata.yumns);
        String version = encodeVersion(p);
        String arch = p.getChildText("arch", Repodata.yumns);
        ContentSourcePackageDetailsKey key = new ContentSourcePackageDetailsKey(name, version, "rpm", arch, "Linux",
            "Platforms");
        ContentSourcePackageDetails pkg = new ContentSourcePackageDetails(key);
        String filename = filename(p);
        pkg.setDisplayName(filename);
        pkg.setShortDescription(p.getChildText("summary", Repodata.yumns));
        pkg.setLongDescription(p.getChildText("description", Repodata.yumns));
        pkg.setFileName(filename);
        pkg.setFileSize(Long.parseLong(p.getChild("size", Repodata.yumns).getAttributeValue("package")));
        pkg.setFileCreatedDate(filedate(p));
        pkg.setLicenseName(p.getChild("format", Repodata.yumns).getChildText("license", Repodata.rpmns));
        pkg.setSHA265(p.getChildText("checksum", Repodata.yumns));
        pkg.setLocation(location(p));
        pkg.setMetadata(gzip(p));
        return pkg;
    }

    /**
     * Compress (using gzip) a string representation of the node.
     *
     * @param  p A package element.
     *
     * @return A gzipped string representation of the node.
     *
     * @throws IOException
     */
    private byte[] gzip(Element p) throws IOException {
        XMLOutputter printer = new XMLOutputter();
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(zipped);
        printer.output(p, gzip);
        gzip.flush();
        gzip.close();
        return zipped.toByteArray();
    }
}