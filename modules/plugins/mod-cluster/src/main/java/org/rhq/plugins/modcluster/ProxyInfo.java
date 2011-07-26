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

    private Map<String, Node> availableNodes = new HashMap<String, Node>();
    private Map<String, Vhost> availableVhosts = new HashMap<String, Vhost>();
    private List<Context> availableContexts = new ArrayList<ProxyInfo.Context>();

    public ProxyInfo(String rawProxyInfo) {
        parseNodes(rawProxyInfo);
        parseVhosts(rawProxyInfo);
        parseContexts(rawProxyInfo);
    }

    private void parseNodes(String rawProxyInfo) {
        Pattern nodePattern = Pattern.compile("Node:.*\n");
        Matcher nodeMatcher = nodePattern.matcher(rawProxyInfo);
        while (nodeMatcher.find()) {
            String rawNode = nodeMatcher.group();
            String[] nodePieces = rawNode.split(",");

            String identifier = nodePieces[0];
            identifier = identifier.substring(identifier.indexOf("[") + 1, identifier.indexOf("]"));

            String jvmRoute = nodePieces[1];
            jvmRoute = jvmRoute.substring(jvmRoute.indexOf(':')).trim();

            availableNodes.put(jvmRoute, new Node(jvmRoute, identifier));
        }
    }

    private void parseVhosts(String rawProxyInfo) {
        Pattern vhostPattern = Pattern.compile("Vhost:.*\n");
        Matcher vhostMatcher = vhostPattern.matcher(rawProxyInfo);
        while (vhostMatcher.find()) {
            String rawVhost = vhostMatcher.group();
            String[] vhostPieces = rawVhost.split(",");

            String identifier = vhostPieces[0].trim();
            identifier = identifier.substring(identifier.indexOf("[") + 1, identifier.indexOf("]"));
            identifier = identifier.substring(0, identifier.lastIndexOf(":"));

            String host = vhostPieces[1].trim();
            host = host.substring(host.indexOf(":") + 1).trim();

            availableVhosts.put(identifier, new Vhost(identifier, host));
        }
    }

    private void parseContexts(String rawProxyInfo) {
        Pattern contextPattern = Pattern.compile("Context:.*[\n|}]");
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

            Vhost relatedVhost = availableVhosts.get(identifier);

            availableContexts.add(new Context("", relatedVhost.getHost(), actualContext, isEnabled));
        }
    }

    public List<Context> getAvailableContexts() {
        return Collections.unmodifiableList(availableContexts);
    }

    public List<Vhost> getAvailableVhosts() {
        return Collections.unmodifiableList(new ArrayList<Vhost>(availableVhosts.values()));
    }

    public List<Node> getAvailableNodes() {
        return Collections.unmodifiableList(new ArrayList<Node>(availableNodes.values()));
    }

    public static class Node {
        private String jvmRoute;
        private String nodeId;

        public Node(String jvmRoute, String nodeId) {
            this.jvmRoute = jvmRoute;
            this.nodeId = nodeId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getJvmRoute() {
            return jvmRoute;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((jvmRoute == null) ? 0 : jvmRoute.hashCode());
            result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
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
            Node other = (Node) obj;
            if (jvmRoute == null) {
                if (other.jvmRoute != null)
                    return false;
            } else if (!jvmRoute.equals(other.jvmRoute))
                return false;
            if (nodeId == null) {
                if (other.nodeId != null)
                    return false;
            } else if (!nodeId.equals(other.nodeId))
                return false;
            return true;
        }
    }

    public static class Vhost {
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
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
            Vhost other = (Vhost) obj;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (identifier == null) {
                if (other.identifier != null)
                    return false;
            } else if (!identifier.equals(other.identifier))
                return false;
            return true;
        }
    }

    public static class Context {
        private String path;
        private String host;
        private String jvmRoute;
        private boolean isEnabled;

        public Context(String jvmRoute, String host, String path) {
            this(jvmRoute, host, path, false);
        }

        public Context(String jvmRoute, String host, String path, boolean isEnabled) {
            this.jvmRoute = jvmRoute;
            this.host = host;
            this.path = path;
            this.isEnabled = isEnabled;
        }

        public String getPath() {
            return path;
        }

        public String getHost() {
            return host;
        }

        public String getJvmRoute() {
            return jvmRoute;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public String createKey() {
            return jvmRoute + ":" + host + ":" + path;
        }

        public String createName() {
            return host + ":" + path;
        }

        @Override
        public String toString() {
            return createKey();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + ((jvmRoute == null) ? 0 : jvmRoute.hashCode());
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
            if (jvmRoute == null) {
                if (other.jvmRoute != null)
                    return false;
            } else if (!jvmRoute.equals(other.jvmRoute))
                return false;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }

        public static Context fromString(String stringRepresentation) throws Exception {
            String parts[] = stringRepresentation.trim().split(":");
            if (parts.length < 3) {
                throw new Exception("Parsing error. Not enough information to create a context.");
            }

            return new Context(parts[0], parts[1], parts[2]);
        }
    }
}