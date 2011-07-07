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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser for the raw proxy information provided by mod_cluster.
 * 
 * @author Stefan Negrea
 */
public class ProxyInfo {

    private List<Context> availableContexts = new ArrayList<ProxyInfo.Context>();
    private Map<String, Vhost> availableVHosts = new HashMap<String, Vhost>();

    public ProxyInfo(String rawProxyInfo) {
        Pattern vhostPattern = Pattern.compile("Vhost.*\n");
        Matcher vhostMatcher = vhostPattern.matcher(rawProxyInfo);
        while (vhostMatcher.find()) {
            String rawVhost = vhostMatcher.group();
            String[] vhostPieces = rawVhost.split(",");

            String identifier = vhostPieces[0].trim();
            identifier = identifier.substring(identifier.indexOf("[") + 1, identifier.indexOf("]"));
            identifier = identifier.substring(0, identifier.lastIndexOf(":"));

            String host = vhostPieces[1].trim();
            host = host.substring(host.indexOf(":") + 1).trim();

            System.out.println(identifier + "--" + host);
            availableVHosts.put(identifier, new Vhost(identifier, host));

        }

        Pattern contextPattern = Pattern.compile("Context.*[\n|}]");
        Matcher contextMatcher = contextPattern.matcher(rawProxyInfo);
        while (contextMatcher.find()) {
            String rawContext = contextMatcher.group();
            String[] contextPieces = rawContext.split(",");
            String actualContext = contextPieces[1].substring(contextPieces[1].indexOf("/")).trim();

            String identifier = contextPieces[0];
            identifier = identifier.substring(identifier.indexOf("[") + 1, identifier.indexOf("]"));
            identifier = identifier.substring(0, identifier.lastIndexOf(":"));
            Vhost relatedVhost = availableVHosts.get(identifier);

            availableContexts.add(new Context(relatedVhost.getHost(), actualContext));
        }
    }

    public List<Context> getAvailableContexts() {
        return Collections.unmodifiableList(availableContexts);
    }

    private static class Vhost {
        private String identifier;
        private String host;

        public Vhost(String identifier, String host) {
            this.identifier = identifier;
            this.host = host;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getHost() {
            return host;
        }

    }

    public static class Context {
        String path;
        String host;

        public Context(String host, String path) {
            this.path = path;
            this.host = host;
        }

        public String getPath() {
            return path;
        }

        public String getHost() {
            return host;
        }

        @Override
        public String toString() {
            return host + ":" + path;
        }

        public static Context fromString(String stringRepresentation) {
            String part[] = stringRepresentation.trim().split(":");
            return new Context(part[0], part[1]);
        }
    }
}