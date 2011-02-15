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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;

/**
 * Discover subsystems
 *
 * @author Heiko W. Rupp
 */
public class SubsystemDiscovery implements ResourceDiscoveryComponent<BaseComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);


        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();


        Configuration config = context.getDefaultPluginConfiguration();
        String cpath = config.getSimpleValue("path", null);
        boolean recursive = false;

        String path;
        if (cpath.endsWith("/*")) {
            path = cpath.substring(0,cpath.length()-2);
            recursive = true;
        }
        else
            path = cpath;


        JSONObject o = connection.getLevelData(path,recursive, false);
        if (!connection.isErrorReply(o)) {
            if (recursive) {
                int i = path.lastIndexOf("/");
                String subPath = path.substring(i+1);

                o = o.getJSONObject(subPath);

                Iterator keyIter = o.keys() ;
                while (keyIter.hasNext()) {

                    String key = (String) keyIter.next();

                    String resKey = context.getParentResourceContext().getResourceKey() + "/" + key;
                    String name = resKey.substring(resKey.lastIndexOf("/") + 1);


                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                            context.getResourceType(), // Type
                            path + "/" + key, // Key
                            name, // Name
                            null, // Version
                            path, // Description
                            config,
                            null);
                    details.add(detail);
                }

            }
            else {


                String resKey = context.getParentResourceContext().getResourceKey();
                String name = resKey.substring(resKey.lastIndexOf("/") + 1);


                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        context.getResourceType(), // Type
                        path, // Key
                        name, // Name
                        null, // Version
                        path, // Description
                        config,
                        null);
                details.add(detail);
            }

            return details;
        }

        return Collections.emptySet();
    }

}
