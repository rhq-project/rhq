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
package org.rhq.plugins.modcluster;

import java.util.HashSet;
import java.util.Set;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.modcluster.helper.JBossHelper;
import org.rhq.plugins.modcluster.model.ProxyInfo;

/**
 * Discovers mod_cluster contexts from the proxyInfo mbean property.
 *
 * @author Stefan Negrea
 */
public class WebappContextDiscoveryComponent implements ResourceDiscoveryComponent<MBeanResourceComponent<?>> {

    private static final String JVM_ROUTE_PROPERTY = "jvmRoute";
    private static final String ENGINE_OBJECT_NAME = "engineObjectName";

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MBeanResourceComponent<?>> context) {
        String jvmRoute = this.getJvmRoute(context);

        EmsBean configBean = context.getParentResourceComponent().getEmsBean();
        String rawProxyInfo = JBossHelper.getRawProxyInfo(configBean);

        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);
        Set<DiscoveredResourceDetails> entities = new HashSet<DiscoveredResourceDetails>();

        for (ProxyInfo.Context availableContext : proxyInfo.getAvailableContexts()) {
            if (availableContext.getJvmRoute().equals(jvmRoute)) {
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(),
                    availableContext.createKey(), availableContext.createName(), null, "Webapp Context", null, null);
                entities.add(detail);
            }
        }

        return entities;
    }

    /**
     * Retrieves the jvm route for the node from the parent context.
     * 
     * @param context the discovery context
     * @return node's jvm route
     */
    private String getJvmRoute(ResourceDiscoveryContext<MBeanResourceComponent<?>> context) {
        Configuration pluginConfig = context.getParentResourceComponent().getResourceContext().getPluginConfiguration();
        String engineObjectName = pluginConfig.getSimple(ENGINE_OBJECT_NAME).getStringValue();
        EmsBean engineBean = this.loadBean(engineObjectName, context.getParentResourceComponent());

        return (String) engineBean.getAttribute(JVM_ROUTE_PROPERTY).refresh().toString();
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     * 
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    protected EmsBean loadBean(String objectName, MBeanResourceComponent context) {
        EmsConnection emsConnection = context.getEmsConnection();
        EmsBean bean = emsConnection.getBean(objectName);
        if (bean == null) {
            // In some cases, this resource component may have been discovered by some means other than querying its
            // parent's EMSConnection (e.g. ApplicationDiscoveryComponent uses a filesystem to discover EARs and
            // WARs that are not yet deployed). In such cases, getBean() will return null, since EMS won't have the
            // bean in its cache. To cover such cases, make an attempt to query the underlying MBeanServer for the
            // bean before giving up.
            emsConnection.queryBeans(objectName);
            bean = emsConnection.getBean(objectName);
        }
        return bean;
    }
}