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
package org.rhq.plugins.sonarqube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Jeremie Lagarde
 */
public class SonarQubeJSONUtility {

    private static final Log LOG = LogFactory.getLog(SonarQubeJSONUtility.class);

    public static String getVersion(String path) throws JSONException {
        return getData(path, "server").getString("version");
    }

    public static String getStatus(String path) throws JSONException {
        return getData(path, "server").getString("status");
    }

    public static JSONObject getData(String path, String query) {
        try {
            return new JSONObject(call(path, query));
        } catch (JSONException e) {
            LOG.warn(e);
        }
        return null;
    }

    public static JSONArray getDatas(String path, String query) {
        try {
            return new JSONArray(call(path, query));
        } catch (JSONException e) {
            LOG.warn(e);
        }
        return null;
    }

    private static String call(String path, String query) {
        URL url = null;
        try {

            path = path.replaceAll(" ", "%20");
            if (!path.endsWith("/"))
                path = path + "/";
            url = new URL(path + "api/" + query);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder(2048);

            while (true) {

                String line = br.readLine();
                if (line == null) {
                    break;
                } else {
                    builder.append(line);
                }
            }

            return builder.toString();
        } catch (MalformedURLException e) {
            LOG.warn(e);
        } catch (IOException e) {
            LOG.warn(e);
        }
        return null;
    }
}
