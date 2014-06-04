/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.modules.plugins.jbossas7.ASConnection.verbose;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Libor Zoubek
 */
public class DomainDeploymentDiscovery implements ResourceDiscoveryComponent<BaseComponent<?>> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        HostControllerComponent<?> hostController = (HostControllerComponent<?>) context.getParentResourceComponent();

        if (!hostController.isDomainController()) {
            return details;
        }

        ASConnection connection = hostController.getASConnection();

        Configuration config = context.getDefaultPluginConfiguration();
        String confPath = config.getSimpleValue("path", "");
        if (confPath == null || confPath.isEmpty()) {
            log.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() + "].");
            return details;
        }

        // Construct the full path including the parent
        String path;
        String parentPath = hostController.getPath();
        if (parentPath == null || parentPath.isEmpty()) {
            parentPath = "";
        }
        path = parentPath;

        if (verbose) {
            log.info("total path: [" + path + "]");
        }

        Address addr = new Address(parentPath);
        Result result = connection.execute(new ReadChildrenNames(addr, confPath));

        if (result.isSuccess()) {

            @SuppressWarnings("unchecked")
            List<String> deployments = (List<String>) result.getResult();

            // There may be multiple children of the given type
            for (String val : deployments) {

                String newPath = confPath + "=" + val;
                Configuration config2 = context.getDefaultPluginConfiguration();

                String resKey;

                if (path == null || path.isEmpty())
                    resKey = newPath;
                else {
                    if (path.startsWith(","))
                        path = path.substring(1);
                    resKey = path + "," + confPath + "=" + val;
                }

                PropertySimple pathProp = new PropertySimple("path", resKey);
                config2.put(pathProp);

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                    resKey, // Key
                    val, // Name
                    null, // Version
                    context.getResourceType().getDescription(), config2, null);
                details.add(detail);
            }
        }

        return details;
    }
}
