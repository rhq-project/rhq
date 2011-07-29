/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.json;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * One Socket binding group of a domain
 * @author Heiko W. Rupp
 */
public class SocketBindingGroup {


    public String name;
    @JsonProperty("default-interface") public String defaultInterface;
    @JsonProperty("port-offset") public int portOffset;
    @JsonProperty("socket-binding") public Map<String,Binding> bindings;


    public static class Binding {
        public String name;
        @JsonProperty("interface") public String iFace;
        public int port;
        @JsonProperty("fixed-port") public Integer fixedPort;
        @JsonProperty("multicast-address") public String mcastAddress;
        @JsonProperty("multicast-port") public Integer mcastPort;

    }
}
