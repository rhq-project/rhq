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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataParser;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceType;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ResourceTypes {

    private List<ResourceType> resourceTypes;
    
    public ResourceTypes(String pluginUri) throws Exception {
        this.resourceTypes = getResourceTypesInPlugin(pluginUri);
    }
    
    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }
    
    public ResourceType findByName(String resourceTypeName) {
        for (ResourceType rt : resourceTypes) {
            if (resourceTypeName.equals(rt.getName())) {
                return rt;
            }
        }
    
        return null;
    }
    
    private static List<ResourceType> getResourceTypesInPlugin(String pluginUri) throws Exception {
        PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(new URI(pluginUri).toURL());
        PluginMetadataParser parser = new PluginMetadataParser(descriptor,
            Collections.<String, PluginMetadataParser> emptyMap());
    
        return parser.getAllTypes();
    }

}
