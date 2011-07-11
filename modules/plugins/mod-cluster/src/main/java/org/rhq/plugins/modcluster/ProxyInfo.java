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

            String rawIsEnabled = contextPieces[2];
            rawIsEnabled = rawIsEnabled.substring(rawIsEnabled.indexOf(':') + 1).trim();
            boolean isEnabled = rawIsEnabled.equals("ENABLED") ? true : false;

            Vhost relatedVhost = availableVHosts.get(identifier);

            availableContexts.add(new Context(relatedVhost.getHost(), actualContext, isEnabled));
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
        private String path;
        private String host;
        private boolean isEnabled;

        public Context(String host, String path) {
            this(path, host, false);
        }

        public Context(String host, String path, boolean isEnabled) {
            this.path = path;
            this.host = host;
            this.isEnabled = isEnabled;
        }

        public String getPath() {
            return path;
        }

        public String getHost() {
            return host;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        @Override
        public String toString() {
            return host + ":" + path;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Context other = (Context) obj;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }

        public static Context fromString(String stringRepresentation) {
            String part[] = stringRepresentation.trim().split(":");
            return new Context(part[1], part[0]);
        }
    }
}