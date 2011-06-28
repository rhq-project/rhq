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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Connection for uploading of content.
 * Partially taken from https://github.com/jbossas/jboss-as/blob/master/testsuite/smoke/src/test/java/org/jboss/as/test/surefire/servermodule/HttpDeploymentUploadUnitTestCase.java
 *
 * @author Jonathan Pearlin (of the original code)
 * @author Heiko W. Rupp
 */
public class ASUploadConnection {

    private static final String BOUNDARY_PARAM = "NeAG1QNIHHOyB5joAS7Rox!!";

    private static final String BOUNDARY = "--" + BOUNDARY_PARAM;

    private static final String CRLF = "\r\n";

    private static final String POST_REQUEST_METHOD = "POST";

    private static final String UPLOAD_URL_PATH = "/management/add-content";

    private final Log log = LogFactory.getLog(ASUploadConnection.class);

    BufferedOutputStream os = null;
    InputStream is = null;
    private HttpURLConnection connection;
    private String host;
    private int port;

    public ASUploadConnection(String dcHost, int port) {
        this.host = dcHost;
        this.port = port;
    }

    public OutputStream getOutputStream(String fileName) {
         try {
            // Create the HTTP connection to the upload URL
            String url = "http://" + host + ":" + port + UPLOAD_URL_PATH;
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(POST_REQUEST_METHOD);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_PARAM);

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
        JsonNode tree = null;
        try {
            os.write(buildPostRequestFooter());
            os.flush();

            int code = connection.getResponseCode();
            System.err.println("Response code " + code);
            if (code==500)
                is = connection.getErrorStream();
            else
                is = connection.getInputStream();

            if (is != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }

                ObjectMapper mapper = new ObjectMapper();

                String s = builder.toString();
                if (s!=null)
                    tree = mapper.readTree(s);
                else
                    System.out.println("- no result received from InputStream -");
            }
            else
                System.out.println("- no InputStream available -");

        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        finally {
            closeQuietly(is);
            closeQuietly(os);
        }

        return tree;
    }


    private byte[] buildPostRequestHeader(String fileName) throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();
        builder.append(buildPostRequestHeaderSection("form-data; name=\"file\"; filename=\""+fileName+"\"", "application/octet-stream", ""));
        return builder.toString().getBytes("US-ASCII");
    }

    private StringBuilder buildPostRequestHeaderSection(final String contentDisposition, final String contentType, final String content) {
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
        return builder;
    }

    private byte[] buildPostRequestFooter() throws UnsupportedEncodingException{
        final StringBuilder builder = new StringBuilder();
        builder.append(CRLF);
        builder.append(BOUNDARY);
        builder.append("--");
        builder.append(CRLF);
        return builder.toString().getBytes("US-ASCII");
    }


    private void closeQuietly(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {}
        }
    }
}
