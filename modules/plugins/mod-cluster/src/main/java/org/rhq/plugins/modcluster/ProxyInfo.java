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

public class ProxyInfo {

    private List<Context> availableContexts = new ArrayList<ProxyInfo.Context>();

    public ProxyInfo(String rawProxyInfo) {

        System.out.println("--------------------------------------");
        System.out.println(rawProxyInfo);
        System.out.println("--------------------------------------");

        Pattern test = Pattern.compile("Context.*\n");
        Matcher m = test.matcher(rawProxyInfo);
        while (m.find()) {
            String rawContext = m.group();
            String[] contextPieces = rawContext.split(",");
            String actualContext = contextPieces[1].substring(contextPieces[1].indexOf("/")).trim();

            availableContexts.add(new Context(actualContext, "localHost"));
        }
    }

    public List<Context> getAvailableContexts() {
        return Collections.unmodifiableList(availableContexts);
    }

    public class Context {
        String path;
        String host;

        public Context(String path, String host) {
            super();
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
    }
}