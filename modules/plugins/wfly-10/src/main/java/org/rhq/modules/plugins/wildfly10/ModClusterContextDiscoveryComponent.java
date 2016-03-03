/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;
import org.rhq.modules.plugins.wildfly10.modcluster.ProxyInfo;

/**
 * Discovers mod_cluster contexts using the proxyInfo details from as7.
 *
 * @author Simeon Pinder
 */
public class ModClusterContextDiscoveryComponent extends SubsystemDiscovery {

    private final Log log = LogFactory.getLog(this.getClass());
    private static Log staticLogger = LogFactory.getLog(ModClusterContextDiscoveryComponent.class);
    private final String PROXY_INFO_OPERATION = "read-proxies-info";
    private final String CONFIGURATION = ",mod-cluster-config=configuration";
    private static String jvmRoute = null;

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context) {

        //initialize discovered list
        Set<DiscoveredResourceDetails> entities = new HashSet<DiscoveredResourceDetails>();

        //BZ:823624: Leave the following blank until this BZ and JIRA 4847 have been resolved.
        if (jvmRoute == null) {//lazy load it.
        //            jvmRoute = "00fa6bff-83a2-3bde-9e76-998b1a4a8c2c";
            jvmRoute = "";
        }

        //Retrieve rawProxyInfo to determine jvmRoute instances
        String rawProxyInfo = "";

        //Retrieve as7 and modcluster components
        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();
        //Should be modcluster element for standalone or managed instances.
        String path = parentComponent.getPath();
        //remove configuration portion
        int located = -1;
        if ((located = path.indexOf(CONFIGURATION)) > -1) {
            path = path.substring(0, located);
        }
        if (path == null || path.isEmpty()) {
            log.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() + "].");
            return entities;
        }

        Address addr = new Address(path);
        Operation op = new Operation(PROXY_INFO_OPERATION, addr);
        Result result = connection.execute(op);
        //get ProxyInfo and parse
        rawProxyInfo = extractRawProxyInfo(result);
        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);

        //only discover the virtualhosts and webcontexts relevant to this modcluster node.
        for (ProxyInfo.Context availableContext : proxyInfo.getAvailableContexts()) {
            if (availableContext.getJvmRoute().equals(jvmRoute)) {
                //prepend the modcluster component to the webcontext key for identification later
                String resourceKey = path + ":" + availableContext.createKey();
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(),
                    resourceKey, availableContext.createName(), null, "Webapp Context", null, null);
                entities.add(detail);
            }
        }
        return entities;
    }

    /** Extracts the ProxyInformation details as returned by AS7 CLI.
     *  Assumes that result.getResult() is of type ArrayList.
     * @param result
     * @return
     */
    static String extractRawProxyInfo(Result result) {
        String rawProxyInfo = "";
        if (result != null && result.isSuccess()) {
            //in invalid type, log and bail.
            if (!(result.getResult() instanceof ArrayList)) {
                staticLogger.warn("Attempting to extract proxyInfo but JSON type information is not correct.");
                return rawProxyInfo;
            }

            //Extract just the values portion Ex. returns "{proxyList},{value}:
            ArrayList container = (ArrayList) result.getResult();
            if ((container != null) && !container.isEmpty()) {
                Object type = container.get(0);
                String values = "";
                if (type instanceof String) {
                    //We only need the value element as the rest is extra.
                    values += container.get(1);
                } else {
                    values = container.toString();
                }
                rawProxyInfo = values;
            }
        }
        return rawProxyInfo;
    }
}