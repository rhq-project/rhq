/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.REUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

/**
 * Connection for uploading of content.
 * Partially taken from https://github.com/jbossas/jboss-as/blob/master/testsuite/smoke/src/test/java/org/jboss/as/test/surefire/servermodule/HttpDeploymentUploadUnitTestCase.java
 *
 * @author Jonathan Pearlin (of the original code)
 * @author Heiko W. Rupp
 */
public class ASUploadConnection {

    private static final String BOUNDARY = "-----------------------------261773107125236";

    private static final String CRLF = "\r\n";

    private static final String POST_REQUEST_METHOD = "POST";

    private static final String UPLOAD_URL = "http://localhost:9990/domain-api/add-content";

    private final Log log = LogFactory.getLog(ASUploadConnection.class);

    BufferedOutputStream os = null;
    BufferedInputStream is = null;
    private HttpURLConnection connection;

    public OutputStream getOutputStream(String fileName) {
         try {
            // Create the HTTP connection to the upload URL
            connection = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST_METHOD);

            // Grab the test WAR file and get a stream to its contents to be included in the POST.
            os = new BufferedOutputStream(connection.getOutputStream());
            os.write(buildPostRequestHeader(fileName));

             return os;
         }
         catch (Exception e) {
             e.printStackTrace();
         }

        return null;
    }

    public JsonNode finishUpload()  {
        try {
            os.write(buildPostRequestFooter());
            os.flush();

            is = new BufferedInputStream(connection.getInputStream());
            // TODO read from IN

        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        finally {
            closeQuietly(is);
            closeQuietly(os);
        }

        return null;
    }


    private byte[] buildPostRequestHeader(String fileName) {
        final StringBuilder builder = new StringBuilder();
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test1\"", "", "test1"));
        builder.append(buildPostRequestHeaderSection("form-data; name=\"test2\"", "", "test2"));
        builder.append(buildPostRequestHeaderSection("form-data; name=\"file\"; filename=\""+fileName+"\"", "application/octet-stream", ""));
        return builder.toString().getBytes();
    }

    private String buildPostRequestHeaderSection(final String contentDisposition, final String contentType, final String content) {
        final StringBuilder builder = new StringBuilder();
        builder.append(BOUNDARY);
        builder.append(CRLF);
        if(contentDisposition != null && contentDisposition.length() > 0) {
            builder.append(String.format("Content-Disposition: %s", contentDisposition));
        }
        builder.append(CRLF);
        if(contentType != null && contentType.length() > 0) {
            builder.append(String.format("Content-Type: %s", contentType));
        }
        builder.append(CRLF);
        if(content != null && content.length() > 0) {
            builder.append(content);
        }
        builder.append(CRLF);
        return builder.toString();
    }

    private byte[] buildPostRequestFooter() {
        final StringBuilder builder = new StringBuilder();
        builder.append(CRLF);
        builder.append(BOUNDARY);
        builder.append("--");
        builder.append(CRLF);
        return builder.toString().getBytes();
    }


    private void closeQuietly(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {}
        }
    }
}
