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
package org.rhq.plugins.apache.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class that contains methods that send http requests and evaluate results
 * @author Ian Springer
 */
public abstract class WWWUtils {

    /**
     * Sends a HEAD request to the passed url and returns if the server was reachable
     * @param  httpURL a http or https URL to check
     * @return true if connecting to the URL succeeds, or false otherwise
     */
    public static boolean isAvailable(URL httpURL) {
        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            // get the respone code to actually trigger sending the Request.
            connection.getResponseCode();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Get the content of the 'Server:' header
     * @param httpURL a http or https URL to get the header from
     * @return The contents of the header or null if anything went wrong or the field was not present.
     */
    public static String getServerHeader(URL httpURL) {
        String ret;

        try {
            HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.connect();
            // get the respone code to actually trigger sending the Request.
            connection.getResponseCode();
            ret = connection.getHeaderField("Server");
        }
        catch (IOException e) {
            ret = null;
        }
        return ret;
    }
}