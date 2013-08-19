/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
public class UrlReader implements RepoReader {

    protected final URL baseUrl;

    public static UrlReader fromUri(URI uri, String username, String password) throws MalformedURLException {
        if (uri.getScheme() == null) {
            try {
                return new DiskReader(new URI("file", uri.getSchemeSpecificPart(), uri.getFragment()).toURL());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(
                    "URI syntax exception while adding the 'file' scheme to a path. This should not have happened.", e);
            }
        } else if (uri.getScheme().equals("file")) {
            return new DiskReader(uri.toURL());
        } else if (uri.getScheme().startsWith("http")) {
            return new HttpReader(uri.toURL(), username, password);
        } else {
            return new UrlReader(uri.toURL());
        }
    }

    protected UrlReader(URL baseUrl) {
        this.baseUrl = baseUrl;

    }

    public void validate() throws IOException, URISyntaxException {
        InputStream content = doOpen(baseUrl);
        content.close();
    }

    /**
     * Open an input stream to specifed relative url. Prepends the baseurl to the <i>url</i> and opens and opens and
     * input stream. Files with a .gz suffix will be unziped (inline).
     *
     * @param  path A path that is relative to the <i>baseurl</i> and references a file within the repo.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException On all errors.
     */
    @Override
    public final InputStream openStream(String path) throws IOException {
        URL url = extendBaseUrl(path);

        InputStream ret = doOpen(url);
        if (path.endsWith(".gz")) {
            ret = new GZIPInputStream(ret);
        }

        return ret;
    }

    protected InputStream doOpen(URL url) throws IOException {
        return url.openStream();
    }

    /**
     * Mainly used for test purposes, othewise not really useful.
     */
    public URL getBaseURL() {
        return baseUrl;
    }

    protected URL extendBaseUrl(String suffix) throws MalformedURLException {
        if (suffix != null) {
            suffix = suffix.trim();
        }

        return suffix == null ? baseUrl : new URL(baseUrl + "/" + suffix);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + baseUrl;
    }
}
