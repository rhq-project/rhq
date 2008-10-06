 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform.content.yum;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a yum/http request made by a yum client.
 *
 * @author jortel
 */
class Request {
    /**
     * The yum server.
     */
    final YumServer server;

    /**
     * An open client (accepted socket)
     */
    private final Socket socket;

    /**
     * A yum repomd metadata object.
     */
    private final Repomd repomd;

    /**
     * A current yum primary metadata object.
     */
    private final Primary primary;

    /**
     * The http <i>filename</i> contained in the URL for the request.
     */
    String filename;

    /**
     * The http <i>args</i> contained in the URL for the request.
     */
    Map<String, String> args;

    /**
     * The http <i>header</i> fields.
     */
    Map<String, String> fields;

    private final Log log = LogFactory.getLog(Request.class);

    /**
     * Constructed with the server and client socket.
     *
     * @param server The yum server that accepted the request.
     * @param socket An open/conditioned client socket.
     */
    Request(YumServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        repomd = new Repomd(this);
        primary = new Primary(this);
    }

    /**
     * Returns the current yum context.
     *
     * @return A yum context.
     */
    YumContext context() {
        return server.context;
    }

    /**
     * Process the yum (http) request. This class provides bare-bones http request processing.
     *
     * <p/>* Read and process the header into a map of header fields and args..
     *
     * <p/>* Reply to the request.
     */
    void process() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String request = reader.readLine();
            StringTokenizer st = new StringTokenizer(request);
            st.nextToken(); // skip request-type
            String url = st.nextToken();
            log.info("processing: " + url);
            String path = url.substring(context().basepath().length() + 1);
            int qmark = qmark(path);
            filename = path.substring(0, qmark);
            args = getArgs(path.substring(qmark));
            fields = httpFields(reader);
            reply();
        } catch (Exception e) {
            log.error("request failed:", e);
        } finally {
            close(socket);
        }
    }

    /**
     * Clean the metadata. Delete locally cached metadata files.
     */
    void cleanMetadata() {
        repomd.delete();
        primary.delete();
    }

    /**
     * Return the index of the (?) character in the URL.
     *
     * @param  path A URL path string.
     *
     * @return The index when found, else the string length.
     */
    private int qmark(String path) {
        int qmark = path.indexOf('?');
        return ((qmark == -1) ? path.length() : qmark);
    }

    /**
     * Build and return a map containing the <i>args</i> part of the URL string.
     *
     * @param  path The path segment of the URL string.
     *
     * @return A map containing the URL args.
     */
    private Map<String, String> getArgs(String path) {
        if (path.length() < 4) {
            return Collections.emptyMap();
        }

        path = path.substring(1);
        Map<String, String> result = new HashMap<String, String>();
        for (String arg : path.split(",")) {
            int eq = arg.indexOf("=");
            String name = arg.substring(0, eq);
            String value = arg.substring(eq + 1);
            result.put(name.trim(), value.trim());
        }

        return result;
    }

    /**
     * Reply to the request using the appropriate content object.
     *
     * @throws Exception On all errors.
     */
    private void reply() throws Exception {
        Content content = selectContent(filename);
        if (content == null) {
            content = new Package(this);
        }

        OutputStream ostr = socket.getOutputStream();
        content.writeHeader(ostr);
        content.writeContent(ostr);
        ostr.close();
    }

    /**
     * Select the appropriate content object based on the file specified in the request URL. Selects a metadata object
     * when identified by name, else it is assumed that the cotent is a package request.
     *
     * @param  filename The request protion of the URL string.
     *
     * @return The content object specified by name in the request.
     */
    private Content selectContent(String filename) {
        if (filename.equals("repodata/repomd.xml")) {
            repomd.delete();
            if (primary.stale()) {
                primary.delete();
            }

            return repomd;
        }

        if (filename.equals("repodata/primary.xml")) {
            return primary;
        }

        return null;
    }

    /**
     * Process the http request using the specified reader and build a map containing the names and values of all of the
     * http header fields.
     *
     * @param  reader A reader opened on the http request input stream.
     *
     * @return A map containing the http header fields.
     *
     * @throws Exception On all errors.
     */
    private Map<String, String> httpFields(BufferedReader reader) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        while (true) {
            String line = reader.readLine();
            if (line.length() > 0) {
                String[] pair = line.split(":");
                result.put(pair[0], pair[1].trim());
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Quietly close the specified socket. Mitigates the <u>really</u> annoying pattern by <i>java.net/java.io</i>
     * objects that throw an exception on close() operations which <u>really</u> sucks when trying to close resources in
     * the finally clause.
     *
     * @param socket An open socket.
     */
    void close(Socket socket) {
        try {
            socket.close();
        } catch (Exception e) {
            log.error("close", e);
        }
    }
}