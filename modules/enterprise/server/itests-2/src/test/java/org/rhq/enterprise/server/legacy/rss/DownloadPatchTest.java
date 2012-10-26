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
package org.rhq.enterprise.server.legacy.rss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * [class description]
 *
 * @author     <a href="mailto:jessica.sant@jboss.com">Jessica Sant</a>
 * @deprecated
 */
@Deprecated
public class DownloadPatchTest //extends TestCase
{
    private Log log = LogFactory.getLog(DownloadPatchTest.class.getName());
    private static final String URL = "https://network.staging.jboss.com/jbossnetwork/secureDownload.html";
    private HttpClient client;

    //@Override
    protected void setUp() throws Exception {
        client = new HttpClient();
    }

    //@Override
    protected void tearDown() throws Exception {
        client = null;
    }

    private void assertEquals(Object o1, Object o2) {
    }

    public void testAccessDownloadsNoUser() throws Exception {
        int statusCode = accessDownload("", "", "");
        assertEquals(HttpStatus.SC_FORBIDDEN, statusCode);
    }

    public void testAccessDownloadsNoPassword() throws Exception {
        int statusCode = accessDownload("fmerenda@jboss.org", "", "");
        assertEquals(HttpStatus.SC_FORBIDDEN, statusCode);
    }

    public void testAccessDownloadsInvalidPassword() throws Exception {
        int statusCode = accessDownload("fmerenda@jboss.org", "xxxxx", "");
        assertEquals(HttpStatus.SC_FORBIDDEN, statusCode);
    }

    public void testAccessDownloadsNoSoftware() throws Exception {
        int statusCode = accessDownload("fmerenda@jboss.org", "password", "");
        assertEquals(HttpStatus.SC_NOT_FOUND, statusCode);
    }

    public void testAccessDownloadsInvalidSoftware() throws Exception {
        int statusCode = accessDownload("fmerenda@jboss.org", "password", "xxxx");
        assertEquals(HttpStatus.SC_NOT_FOUND, statusCode);
    }

    public void testAccessDownloadsValidSoftware() throws Exception {
        int fileSize = accessDownloadGetFileSize("fmerenda@jboss.org", "password", "a0450000005izHvAAI");
        assertEquals(330322, fileSize);
    }

    private int accessDownload(String username, String password, String softwareId) throws Exception {
        GetMethod method = new GetMethod(URL);
        method.addRequestHeader("username", username);
        method.addRequestHeader("password", password);
        method.setFollowRedirects(true);

        NameValuePair softwarePair = new NameValuePair("softwareId", softwareId);
        method.setQueryString(new NameValuePair[] { softwarePair });

        try {
            int statusCode = client.executeMethod(method);
            log.debug("Method status: " + method.getStatusLine());
            if (HttpStatus.SC_OK == statusCode) {
                String content = readContent(method);
                log.debug("file length: " + content.length());
            }

            return statusCode;
        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        } finally {
            method.releaseConnection();
        }
    }

    private int accessDownloadGetFileSize(String username, String password, String softwareId) throws Exception {
        GetMethod method = new GetMethod(URL);
        method.addRequestHeader("username", username);
        method.addRequestHeader("password", password);
        method.setFollowRedirects(true);

        NameValuePair softwarePair = new NameValuePair("softwareId", softwareId);
        method.setQueryString(new NameValuePair[] { softwarePair });

        try {
            int contentLength = 0;
            int statusCode = client.executeMethod(method);
            log.debug("Method status: " + method.getStatusLine());
            if (HttpStatus.SC_OK == statusCode) {
                String content = readContent(method);
                contentLength = content.length();
            }

            return contentLength;
        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        } finally {
            method.releaseConnection();
        }
    }

    private String readContent(GetMethod get) throws IOException {
        StringBuffer result = new StringBuffer();
        try {
            Reader input = new InputStreamReader(get.getResponseBodyAsStream(), get.getResponseCharSet());
            BufferedReader bufferedReader = (input instanceof BufferedReader) ? (BufferedReader) input
                : new BufferedReader(input);
            char[] buffer = new char[4 * 1024];
            int charsRead;
            while ((charsRead = bufferedReader.read(buffer)) != -1) {
                result.append(buffer, 0, charsRead);
            }
        } catch (UnsupportedEncodingException e) {
            result.append(get.getResponseBodyAsString());
        }

        return result.toString();
    }
}