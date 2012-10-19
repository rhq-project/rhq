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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

/**
 * Parse a httpd.conf file and provide information about its content
 *
 * @author Heiko W. Rupp
 */
public class HttpdConfParser {

    private final Log log = LogFactory.getLog(HttpdConfParser.class);


    private Set<String> vhosts = new HashSet<String>();
    private boolean modJkInstalled;
    private String workerPropertiesFile;
    private String uriWorkerLocation;
    private String mainServer;



    /**
     * Parses the httpd.conf file located at confPath
     * @param confPath The path to the httpd.conf file
     * @return true on success , false otherwise
     */
    public boolean parse(String confPath) {

        File file = new File(confPath);
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            log.warn("Config file " + confPath + " is not readable, mod_jk can not be detected");
            return false;
        }

        BufferedReader reader=null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            String tmp;
            while ((line=reader.readLine())!=null) {
                line=line.trim();
                tmp = getValueFrom2ndArg(line);

                if (line.startsWith("LoadModule") && line.contains("jk_module")) {
                    modJkInstalled=true;
                }
                else if (line.startsWith("JkWorkersFile")) {
                    if (tmp!=null) {
                        workerPropertiesFile = tmp;
                    }
                }
                else if (line.startsWith("<VirtualHost")) {
                    if (tmp!=null) {
                        vhosts.add(tmp);
                    }
                }
                else if (line.startsWith("JkMountFile")) {
                    if (tmp!=null) {
                        uriWorkerLocation = tmp;
                    }
                }
                else if (line.startsWith("JkMount") && !line.startsWith("JkMountFile")) {
                    // TODO
                }
                else if (line.startsWith("Listen")) {
                    if (tmp!=null) {
                        mainServer=tmp;
                    }
                }
            }
        }
        catch (IOException ioe) {
            log.warn("Can't process " + confPath + " :" + ioe.getMessage());
            return false;
        }
        finally {
            if (reader!=null)
                try {
                    reader.close();
                }
                catch (IOException e) {
                    // Nothing we can do ...
                }
        }

        return true;

    }

    /**
     * Splits the passed string into two parts at the boundary of a space char and returns the second part.
     * @param input Input string
     * @return the second part or null
     */
    @Nullable
    private String getValueFrom2ndArg(String input) {

        if (input==null)
            return null;

        String ret = null;
        String[] tokens = input.split("\\s");
        if (tokens.length>1)
            ret=tokens[1];
        return ret;
    }

    public Set<String> getVhosts() {
        return vhosts;
    }

    public String getWorkerPropertiesFile() {
        return workerPropertiesFile;
    }

    public boolean isModJkInstalled() {
        return modJkInstalled;
    }

    public String getUriWorkerLocation() {
        return uriWorkerLocation;
    }

    public String getMainServer() {
        return mainServer;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("HttpdConfParser");
        sb.append("{listen=").append(mainServer);
        sb.append(", vhosts=").append(vhosts);
        sb.append(", modJkInstalled=").append(modJkInstalled);
        sb.append(", workerPropertiesFile='").append(workerPropertiesFile).append('\'');
        sb.append(", uriWorkerLocation='").append(uriWorkerLocation).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
