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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.Subsystem;

/**
 * Discover the domain
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class DomainDiscovery implements ResourceDiscoveryComponent<BaseComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING,true);

        // TODO get next from some host.xml file
        String host = "localhost";
        String portString = "9990";
        int port = Integer.parseInt(portString);
        ASConnection connection = new ASConnection(host,port);


        Configuration config = context.getDefaultPluginConfiguration();


        // A domain has a server group so check for it.
        boolean found = false;
        JsonNode json = connection.getLevelData(null,null);
        if (!ASConnection.isErrorReply(json)) {

            Iterator<String> fields = json.getFieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (field.equals("server-group"))
                    found=true;

            }

            if (found) {

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        context.getResourceType(), // DataType
                        "Domain", // Key
                        "Domain", // Name
                        null, // Version
                        context.getResourceType().getDescription(), // Description
                        config,
                        null);
                details.add(detail);
            }

            return details;
        }

        return Collections.emptySet();
    }

}
