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
package org.rhq.plugins.postfix;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostfixServerDiscoveryComponent extends AugeasConfigurationDiscoveryComponent {

    private static final Pattern hostNamePattern = Pattern.compile("[\\s]*myhostname[\\s]*=[\\s]*([^$].*)[\\s]*");
    
    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> resources = super.discoverResources(resourceDiscoveryContext);
        for (DiscoveredResourceDetails detail : resources){
            Configuration config = detail.getPluginConfiguration();
            PropertySimple property = (PropertySimple) config.get(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
            String configFilePath = property.getStringValue();
            String resourceName;
            
            try {
              resourceName = findHostName(configFilePath);
            }catch(Exception e){
              resourceName = resourceDiscoveryContext.getSystemInformation().getHostname();    
            }
            detail.setResourceName(resourceName);
        }
        return resources;
    }
    
    private String findHostName(String includeFile) throws Exception{       
        try {
            File file = new File(includeFile);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                try {
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        Matcher m = hostNamePattern.matcher(strLine);
                        if (m.matches()) {
                            String glob = m.group(1);
    
                           return glob;
                        }                   
                    }
                } finally {
                    StreamUtil.safeClose(br);
                }
            }
           }
          catch (Exception e) {
            throw new Exception("NetBios name was not found in configuration file "+ includeFile + " cause:",e);
        }
          throw new Exception("NetBios name was not found in configuration file "+ includeFile);
    }
}
