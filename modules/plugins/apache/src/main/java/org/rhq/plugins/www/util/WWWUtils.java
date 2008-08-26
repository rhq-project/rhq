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
package org.rhq.plugins.www.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Ian Springer
 */
public abstract class WWWUtils {
    /**
     * @param  httpURL an http or https URL
     *
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
}