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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * The Repodata object is used to read yum metadata.
 *
 * @author jortel
 */
public class Repodata {
    /**
     * Yum metadata files types.
     *
     * @author jortel
     */
    enum Meta {
        primary, filelists, other
    };

    /**
     * A yum reader used to access the repo.
     */
    private final RepoReader reader;

    /**
     * A SAX builder.
     */
    private SAXBuilder builder = new SAXBuilder();

    /**
     * The location mapping of metadata types to hrefs.
     */
    private Map<Meta, String> locations = new HashMap<Meta, String>();

    /**
     * A meta document cache. Ensures that metadata files are only read/parsed once.
     */
    private Map<Meta, Element> documents = new HashMap<Meta, Element>();

    /**
     * Namespaces
     */
    public static final Namespace yumns = Namespace.getNamespace("http://linux.duke.edu/metadata/common");

    public static final Namespace repons = Namespace.getNamespace("http://linux.duke.edu/metadata/repo");

    public static final Namespace rpmns = Namespace.getNamespace("rpm", "http://linux.duke.edu/metadata/rpm");

    /**
     * Constructor. Initializes the object and refreshes the metadata caches.
     *
     * @param reader An initialized reader used to access the repo.
     *
     * @see   refresh().
     */
    public Repodata(RepoReader reader) {
        this.reader = reader;
    }

    /**
     * Refresh the repo's metadata.
     *
     * <p/>1) Clear all cached data.
     *
     * <p/>2) Read and process repomd.xml
     *
     * @throws Exception
     */
    public void refresh() throws Exception {
        clear();
        Element repomd = getRepomd();
        for (Meta t : Meta.values()) {
            locations.put(t, getLocation(repomd, t));
        }
    }

    /**
     * Clear caches.
     */
    public void clear() {
        locations.clear();
        documents.clear();
    }

    /**
     * Get the object's reader.
     *
     * @return The object's reader.
     */
    public RepoReader getReader() {
        return reader;
    }

    /**
     * Get and parse the repo's <i>repomd.xml</i> file.
     *
     * @return The root repomd node.
     *
     * @throws Exception On all errors.
     */
    public Element getRepomd() throws Exception {
        InputStream istr = reader.openStream("repodata/repomd.xml");
        return builder.build(istr).getRootElement();
    }

    /**
     * Get the node for the specified metadata type. The node is returned from the cache when already cached. Otherwise,
     * it is read/parsed and stored in the cache.
     *
     * @param  type A metadata type.
     *
     * @return The requested node.
     *
     * @throws Exception On all errors.
     */
    public Element getMetadata(Meta type) throws Exception {
        Element node = documents.get(type);
        InputStream istr = null;
        try {
            if (node == null) {
                String href = locations.get(type);
                istr = reader.openStream(href);
                node = builder.build(istr).getRootElement();
                documents.put(type, node);
            }
        } finally {
            if (istr != null) {
                istr.close();
            }
        }

        return node;
    }

    /**
     * A utility for getting an XML (string representation) for the specified metadata type.
     *
     * @param  type A type of metadata.
     *
     * @return A XML string.
     *
     * @throws Exception On all errors.
     */
    public String toString(Meta type) throws Exception {
        return toString(getMetadata(type));
    }

    /**
     * Get the <location href=""/> value from the repomd node for the specified yum metadata type.
     *
     * @param  repomd A repomd node.
     * @param  type   The yum metadata type.
     *
     * @return The href for the specified type when found, else "".
     */
    @SuppressWarnings("unchecked")
    private String getLocation(Element repomd, Meta type) {
        String href = "";
        for (Element e : (List<Element>) repomd.getChildren()) {
            String typeAttribute = e.getAttributeValue("type");
            if (typeAttribute != null && typeAttribute.equals(type.name())) {
                Element location = e.getChild("location", repons);
                href = location.getAttributeValue("href");
                break;
            }
        }

        return href;
    }

    /**
     * Utility for converting an XML fragment into a string.
     *
     * @param  e An element to convert.
     *
     * @return A string representation of the specified element.
     */
    public static String toString(Element e) {
        Format format = Format.getPrettyFormat();
        XMLOutputter p = new XMLOutputter(format);
        return p.outputString(e);
    }
}