/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.modcluster;

import java.util.Set;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ModClusterDiscoveryComponent extends MBeanResourceDiscoveryComponent {

    private final static String MOD_CLUSTER_CONFIG_FILE = "modclusterConfigFile";
    private static final String JBOSS_AS4_CONFIGURATION_FILE_RELATIVE_PATH = "/deploy/jboss-web.deployer/server.xml";
    private static final String JBOSS_AS5_CONFIGURATION_FILE_RELATIVE_PATH = "/deploy/mod_cluster.sar/META-INF/mod_cluster-jboss-beans.xml";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> tempSet = super.discoverResources(context);

        for (DiscoveredResourceDetails detail : tempSet) {
            String configurationFile = findConfigurationFile(context.getParentResourceComponent());
            detail.getPluginConfiguration().put(new PropertySimple(MOD_CLUSTER_CONFIG_FILE, configurationFile));
        }

        return tempSet;
    }

    @SuppressWarnings("static-access")
    private String findConfigurationFile(ResourceComponent parentResourceComponent) {
        try {
            if (parentResourceComponent instanceof ApplicationServerComponent) {
                ApplicationServerComponent parentComponent = (ApplicationServerComponent) parentResourceComponent;

                return parentComponent.getResourceContext().getPluginConfiguration().getSimple("serverHomeDir")
                    .getStringValue()
                    + JBOSS_AS5_CONFIGURATION_FILE_RELATIVE_PATH;

            }
        } catch (java.lang.NoClassDefFoundError e) {
            //Do absolutely nothing, that means the class loader does not have this module loaded. 
            //Just continue with the next discovery attempt.
        }

        try {
            if (parentResourceComponent instanceof JBossASServerComponent) {
                JBossASServerComponent parentComponent = (JBossASServerComponent) parentResourceComponent;

                return parentComponent.getPluginConfiguration()
                    .getSimple(parentComponent.CONFIGURATION_PATH_CONFIG_PROP).getStringValue()
                    + JBOSS_AS4_CONFIGURATION_FILE_RELATIVE_PATH;
            }
        } catch (java.lang.NoClassDefFoundError e) {
            //Do absolutely nothing, that means the class loader does not have this module loaded.
        }

        return null;
    }
}
