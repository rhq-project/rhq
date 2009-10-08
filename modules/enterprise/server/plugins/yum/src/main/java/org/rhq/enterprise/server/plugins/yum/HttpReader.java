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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * The http reader is a yum repo reader used to read metadata and bits from an existing (remote) yum repo using yum's
 * native http interface.
 *
 * @author jortel
 */
public class HttpReader implements RepoReader {
    /**
     * The base url of a yum repo.
     */
    private final String baseurl;

    /**
     * The current url connection
     */
    HttpURLConnection connection;

    /**
     * Constructor.
     *
     * @param basepath The base url of a yum repo.
     */
    public HttpReader(String baseurl) {
        this.baseurl = baseurl;
    }

    /**
     * Validate the reader. Validates that the base url is valid.
     *
     * @throws Exception When <i>baseurl</i> is not valid.
     */
    public void validate() throws Exception {
        URL url = new URL(baseurl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try {
            if (connection.getHeaderField(0) == null) {
                throw new IOException("Cannot validate connection - check URL");
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Open an input stream to specifed relative url. Prepends the baseurl to the <i>url</i> and opens and opens and
     * input stream. Files with a .gz suffix will be unziped (inline).
     *
     * @param  suffix A url that is relative to the <i>baseurl</i> and references a file within the repo.
     *
     * @return An open input stream that <b>must</b> be closed by the caller.
     *
     * @throws IOException On all errors.
     */
    public InputStream openStream(String suffix) throws IOException {
        URL url = new URL(baseurl + "/" + suffix);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        InputStream in = connection.getInputStream();
        if (suffix.endsWith(".gz")) {
            return new GZIPInputStream(in);
        }

        return in;
    }

    /*
     * (non-Javadoc) @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return baseurl;
    }
}