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
package org.rhq.plugins.modcluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser for the raw proxy information provided by mod_cluster.
 * 
 * @author Stefan Negrea
 */
public class ProxyInfo {

    private List<Context> availableContexts = new ArrayList<ProxyInfo.Context>();

    public ProxyInfo(String rawProxyInfo) {

        Pattern test = Pattern.compile("Context.*\n");
        Matcher m = test.matcher(rawProxyInfo);
        while (m.find()) {
            String rawContext = m.group();
            String[] contextPieces = rawContext.split(",");
            String actualContext = contextPieces[1].substring(contextPieces[1].indexOf("/")).trim();

            availableContexts.add(new Context(actualContext, "localhost"));
        }
    }

    public List<Context> getAvailableContexts() {
        return Collections.unmodifiableList(availableContexts);
    }

    public static class Context {
        String path;
        String host;

        public Context(String path, String host) {
            this.path = path;
            this.host = host;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        @Override
        public String toString() {
            return "Context [path=" + path + ", host=" + host + "]";
        }

        public static Context fromString(String stringRepresentation) {
            stringRepresentation = stringRepresentation.substring(stringRepresentation.indexOf('[') + 1);
            stringRepresentation = stringRepresentation.substring(0, stringRepresentation.indexOf(']'));
            stringRepresentation = stringRepresentation.trim();

            String host = null;
            String path = null;
            for (String part : stringRepresentation.split(",")) {
                part = part.trim();
                if (part.startsWith("path=")) {
                    path = part.substring(5).trim();
                } else if (part.startsWith("host=")) {
                    host = part.substring(5).trim();
                }
            }
            return new Context(path, host);
        }
    }
}