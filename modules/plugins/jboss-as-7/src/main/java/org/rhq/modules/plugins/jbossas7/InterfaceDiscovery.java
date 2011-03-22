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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.Attribute;
import org.rhq.modules.plugins.jbossas7.json.Domain;
import org.rhq.modules.plugins.jbossas7.json.NetworkInterface;

/**
 * Discover subsystems
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class InterfaceDiscovery implements ResourceDiscoveryComponent<BaseComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING,true);

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();



        JsonNode json = connection.getLevelData(null, null);
        if (!connection.isErrorReply(json)) {
            Domain domain = mapper.readValue(json, new TypeReference<Domain>() {});

            for (Map.Entry<String,String> entry: domain.interfaces.entrySet()) {

                String key = entry.getKey();
                String path = "/interface/" + key;
                JsonNode subJson = connection.getLevelData(path);

                NetworkInterface networkInterface = mapper.readValue(subJson, new TypeReference<NetworkInterface>() {});
                networkInterface.name= key;
                for (Map.Entry<String,Attribute> nentry : networkInterface.attributes.entrySet()) {
                    nentry.getValue().name = nentry.getKey();

                    String resKey = context.getParentResourceContext().getResourceKey() + "/" + key;
                    String name = resKey.substring(resKey.lastIndexOf("/") + 1);

                    Configuration config = context.getDefaultPluginConfiguration();

                    PropertySimple propertySimple = new PropertySimple("path",path);
                    config.put(propertySimple);

                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                            context.getResourceType(), // DataType
                            path, // Key
                            name, // Name
                            null, // Version
                            networkInterface.description, // Description
                            config,
                            null);
                    details.add(detail);
                }
            }

            return details;
        }

        return Collections.emptySet();
    }

}
