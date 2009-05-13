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

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Similiar to the {@link UrlSource} content source, but this expects the URL to use
 * the HTTP protocol. This provides the ability to go through an HTTP proxy.
 * 
 * @author John Mazzitelli
 */
public class HttpSource extends UrlSource {
    private final Log log = LogFactory.getLog(HttpSource.class);

    private String username;
    private String password;
    private String proxyUrl;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;

    @Override
    protected void setRootUrl(URL url) {
        if (!url.getProtocol().startsWith("http")) {
            throw new UnsupportedOperationException(
                "This content source cannot support a protocol other than HTTP: url=" + url);
        }

        super.setRootUrl(url);
    }

    @Override
    protected void setIndexUrl(URL url) {
        if (!url.getProtocol().startsWith("http")) {
            throw new UnsupportedOperationException(
                "This content source cannot support a protocol other than HTTP: index-url=" + url);
        }

        super.setIndexUrl(url);
    }

    @Override
    public void initialize(Configuration configuration) throws Exception {
        this.username = configuration.getSimpleValue("username", null);
        this.password = configuration.getSimpleValue("password", null);
        this.proxyUrl = configuration.getSimpleValue("proxyUrl", null);
        this.proxyUsername = configuration.getSimpleValue("proxyUsername", null);
        this.proxyPassword = configuration.getSimpleValue("proxyPassword", null);

        String port = configuration.getSimpleValue("proxyPort", null);
        if (port != null) {
            this.proxyPort = Integer.parseInt(port);
        }

        super.initialize(configuration);

        return;
    }

    @Override
    public void shutdown() {
        this.username = null;
        this.password = null;
        this.proxyUrl = null;
        this.proxyUsername = null;
        this.proxyPassword = null;
        this.proxyPort = 0;

        super.shutdown();

        return;
    }

    @Override
    public void testConnection() throws Exception {
        HttpClient client = new HttpClient();
        HeadMethod method = new HeadMethod(getRootUrl().toString());
        prepareHttpClient(client, method);
        int status = client.executeMethod(method);
        if (status != HttpStatus.SC_OK) {
            throw new Exception("Content source failed connection test with status code=" + status);
        }
        return;
    }

    @Override
    public InputStream getInputStream(String location) throws Exception {
        URL fullUrl = new URL(getRootUrl().toString() + location);
        InputStream stream = getInputStreamForUrl(fullUrl);
        return stream;
    }

    @Override
    protected InputStream getIndexInputStream() throws Exception {
        InputStream stream = getInputStreamForUrl(getIndexUrl());
        return stream;
    }

    /**
     * Given any URL, will return a stream to that URL using the HTTP client and GET method
     * for the authentication as defined in this content source's configuration.
     * 
     * @param url the URL whose stream of content is returned
     *
     * @return stream containing the content for the given URL
     *
     * @throws Exception if cannot get the streamed content
     */
    protected InputStream getInputStreamForUrl(URL url) throws Exception {
        String fullLocation = url.toString();

        HttpClient client = new HttpClient();
        HttpMethodBase method = new GetMethod(fullLocation);
        prepareHttpClient(client, method);
        int status = client.executeMethod(method);

        switch (status) {
        case HttpStatus.SC_OK: {
            break; // good to go
        }

        case HttpStatus.SC_NOT_FOUND: {
            throw new Exception("Could not find the content at URL [" + fullLocation
                + "]. Make sure the content source defines a valid URL.");
        }

        case HttpStatus.SC_UNAUTHORIZED:
        case HttpStatus.SC_FORBIDDEN: {
            throw new Exception("Invalid login credentials specified for user [" + username + "]. Make sure "
                + "this user is valid and the password specified for this content source is correct.");
        }

        default: {
            throw new Exception("Failed to retrieve content. status code=" + status);
        }
        }

        InputStream stream = method.getResponseBodyAsStream();

        return stream;
    }

    /**
     * Given a client and the method to be used by that client, this will prepare those objects
     * so they can be used to get the remote content.
     * 
     * @param client
     * @param method
     * 
     * @throws Exception if the client cannot be prepared successfully
     */
    protected void prepareHttpClient(HttpClient client, HttpMethodBase method) throws Exception {

        // prepare the client with proxy info, if appropriate
        configureProxy(client);

        // setup the authentication
        method.setFollowRedirects(true);

        if (this.username != null) {
            method.setDoAuthentication(true);

            org.apache.commons.httpclient.URI fullUri = method.getURI();
            AuthScope authScope = new AuthScope(fullUri.getHost(), fullUri.getPort(), AuthScope.ANY_REALM);
            Credentials credentials = new UsernamePasswordCredentials(this.username, this.password);
            client.getState().setCredentials(authScope, credentials);
        }

        return;
    }

    /**
     * If proxy information was specified, configures the client to use it.
     *
     * @param client client being used in the invocation
     */
    protected void configureProxy(HttpClient client) {
        if (this.proxyUrl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Configuring HTTP proxy. url [" + this.proxyUrl + "]; port [" + this.proxyPort + "]");
            }

            HostConfiguration hostConfiguration = client.getHostConfiguration();
            hostConfiguration.setProxy(this.proxyUrl, this.proxyPort);

            // If a proxy username was specified, indicate it as the proxy credentials
            if (this.proxyUsername != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Configuring feed for authenticating proxy. proxy-user: " + this.proxyUsername);
                }
                AuthScope proxyAuthScope = new AuthScope(this.proxyUrl, this.proxyPort, AuthScope.ANY_REALM);
                Credentials proxyCredentials = new UsernamePasswordCredentials(this.proxyUsername, this.proxyPassword);
                client.getState().setProxyCredentials(proxyAuthScope, proxyCredentials);
            }
        }

        return;
    }
}
