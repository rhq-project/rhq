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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.Base64;

/**
 * The http reader is a yum repo reader used to read metadata and bits from an existing (remote) yum repo using yum's
 * native http interface.
 *
 * @author jortel
 */
public class HttpReader extends UrlReader {

    private static final Log LOG = LogFactory.getLog(RepoProvider.class);

    private final String username;
    private final String password;

    /**
     * Constructor.
     *
     * @param baseUrl The base url of a yum repo.
     * @param username the name of the user to authenticate with or null
     * @param password the password to use or null
     */
    public HttpReader(URL baseUrl, String username, String password) {
        super(baseUrl);
        this.username = username;
        this.password = password;
    }

    @Override
    protected InputStream doOpen(URL url) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("open " + url);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);

        if (username != null) {
            String userInfo = username;
            if (password != null) {
                userInfo += ":" + password;
            }
            String basicAuth = "Basic " + Base64.encode(userInfo.getBytes("ISO-8859-1"));
            connection.setRequestProperty("Authorization", basicAuth);
        }
        return connection.getInputStream();
    }
}
