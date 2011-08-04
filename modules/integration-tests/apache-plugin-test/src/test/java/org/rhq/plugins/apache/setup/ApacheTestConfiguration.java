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

package org.rhq.plugins.apache.setup;

import java.util.HashMap;
import java.util.Map;

import org.rhq.plugins.apache.ApacheVirtualHostServiceComponent;
import org.rhq.plugins.apache.ApacheVirtualHostServiceDiscoveryComponent;
import org.rhq.plugins.apache.util.VHostSpec;
import org.rhq.plugins.apache.util.ApacheDeploymentUtil.DeploymentConfig;

public class ApacheTestConfiguration {
    public String[] apacheConfigurationFiles;
    public String inventoryFile;
    public String configurationName;
    public String serverRoot;
    public String binPath;
    public Map<String, String> defaultOverrides = new HashMap<String, String>();
    
    public void beforeTestSetup(ApacheTestSetup testSetup) throws Throwable {
        
    }
    
    public void beforePluginContainerStart(ApacheTestSetup setup) throws Throwable {
        
    }
    
    public void beforeTests(ApacheTestSetup setup) throws Throwable {
        
    }
    
    public String[] getExpectedResourceKeysAfterUpgrade(ApacheTestSetup setup) {
        return getFullSuccessfulUpgradeResourceKeys(setup);
    }
    
    public String[] getExpectedResourceKeysWithFailures(ApacheTestSetup setup) {
        return null;
    }
    
    protected String[] getFullSuccessfulUpgradeResourceKeys(ApacheTestSetup setup) {
        DeploymentConfig dc = setup.getDeploymentConfig();
        Map<String, String> replacements = dc.getTokenReplacements();

        VHostSpec vh1 = dc.vhost1.getVHostSpec(replacements);
        VHostSpec vh2 = dc.vhost2.getVHostSpec(replacements);
        VHostSpec vh3 = dc.vhost3.getVHostSpec(replacements);
        VHostSpec vh4 = dc.vhost4.getVHostSpec(replacements);

        String[] ret = new String[5];

        ret[0] = ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY;
        ret[1] = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(vh1.serverName, vh1.hosts);
        ret[2] = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(vh2.serverName, vh2.hosts);
        ret[3] = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(vh3.serverName, vh3.hosts);
        ret[4] = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(vh4.serverName, vh4.hosts);

        return ret;
    }
}