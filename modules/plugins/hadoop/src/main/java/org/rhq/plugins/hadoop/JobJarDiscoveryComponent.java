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

package org.rhq.plugins.hadoop;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class JobJarDiscoveryComponent implements ResourceDiscoveryComponent<JobTrackerServerComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JobTrackerServerComponent> context)
        throws InvalidPluginConfigurationException, Exception {
        
        File dataDir = context.getParentResourceComponent().getJobJarDataDir();
        
        File[] jars = dataDir.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        
        Set<DiscoveredResourceDetails> ret = new HashSet<DiscoveredResourceDetails>();
        
        for(File jar : jars) {
            DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(), jar.getAbsolutePath(), jar.getName(), null, null, context.getDefaultPluginConfiguration(), null);
            ret.add(details);
        }
        
        return ret;
    }

}
