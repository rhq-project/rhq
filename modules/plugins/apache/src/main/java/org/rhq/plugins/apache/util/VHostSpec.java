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

package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.List;

import org.rhq.plugins.apache.ApacheVirtualHostServiceDiscoveryComponent;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;

public class VHostSpec {
    public String serverName;
    public List<String> hosts;
    
    public static List<VHostSpec> detect(ApacheDirectiveTree config) {
        List<ApacheDirective> virtualHosts = config.search("/<VirtualHost");
        
        List<VHostSpec> ret = new ArrayList<VHostSpec>(virtualHosts.size());
        
        for(ApacheDirective dir : virtualHosts) {
            ret.add(new VHostSpec(dir));
        }
        
        return ret;
    }
    
    public VHostSpec(ApacheDirective vhostDirective) {
        hosts = new ArrayList<String>(vhostDirective.getValues());

        List<ApacheDirective> serverNames = vhostDirective.getChildByName("ServerName");
        serverName = null;
        if (serverNames.size() > 0) {
            serverName = serverNames.get(serverNames.size() - 1).getValuesAsString();
        }
    }
    
    @Override
    public String toString() {
        return ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(serverName, hosts);
    }
    
    @Override
    public int hashCode() {
        int ret = serverName != null ? serverName.hashCode() : 1;
        for(String host : hosts) {
            ret = 31 * ret * host.hashCode();
        }
        
        return ret;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof VHostSpec)) {
            return false;
        }
        
        VHostSpec other = (VHostSpec) o;
        
        boolean serverNameEqual = serverName == null ? other.serverName == null : serverName.equals(other.serverName);
        
        if (!serverNameEqual) {
            return false;
        }
        
        return hosts.equals(other.hosts);
    }
}