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
package org.rhq.plugins.hudson;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * @author Greg Hinkle
 */
public class HudsonJSONUtility {

    private static final Log LOG = LogFactory.getLog(HudsonJSONUtility.class);

    public static JSONObject getData(String path, int depth) {

        URL url = null;
        try {

            path = path.replaceAll(" ", "%20");

            url = new URL(path + "/api/json?depth=" + depth);


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

            JSONObject jsonObject = new JSONObject(builder.toString());

            return jsonObject;
        } catch (MalformedURLException e) {
            LOG.warn(e);
        } catch (IOException e) {
            LOG.warn(e);
        } catch (JSONException e) {
            LOG.warn(e);
        }
        return null;


    }

    public static String getVersion(String path) {
        try {
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder(2048);

            while (true) {

                String line = br.readLine();
                if (line == null) {
                    break;
                } else {
                    int verIndex = line.indexOf("Hudson ver. ");
                    if (verIndex >=0) {
                        int endIndex = line.indexOf((int) '<', verIndex);
                        String version = line.substring(verIndex + "Hudson ver. ".length(), endIndex);
                        return version;
                    }
                    builder.append(line);
                }
            }
        } catch (MalformedURLException e) {
            LOG.warn(e);
        } catch (IOException e) {
            LOG.warn(e);
        }
        return null;
    }


    public static void main(String[] args) throws JSONException {
        System.out.println("VERSION: " + getVersion("http://hudson.jboss.org/hudson"));
        


        JSONObject object = getData("http://hudson.jboss.org/hudson/job/DNA continuous on JDK1.5", 0);
        System.out.println("Got it\n" + object.toString(2));

        System.out.println(object.get("description"));
        System.out.println(object.getJSONArray("jobs").length());
        
    }
}
