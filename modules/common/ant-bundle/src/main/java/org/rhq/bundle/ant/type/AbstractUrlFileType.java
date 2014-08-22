/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.bundle.ant.type;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tools.ant.BuildException;

/**
 * A base class for the functionality shared by {@link UrlFileType} and {@link UrlArchiveType}.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public abstract class AbstractUrlFileType extends AbstractBundleType implements HasHandover {
    private String url;
    private URL source;

    /**
     * Returns the base filename of the URL (no parent paths will be in the returned name).
     * If there is no path, the hostname of the URL is used.
     * 
     * @return base filename of the source file
     */
    public String getBaseName() {
        String path = this.source.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length());
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            path = path.substring(lastSlash + 1);
        }
        if (path.length() == 0) {
            path = this.source.getHost();
        }
        return path;
    }

    public URL getSource() {
        return this.source;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String urlString) {
        this.url = urlString;
        try {
            this.source = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new BuildException("URL specified by 'url' attribute [" + urlString
                + "] is malformed - it must be a valid URL.");
        }
    }
}
