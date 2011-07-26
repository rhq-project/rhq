/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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

/**
 * Discovers mod_cluster contexts from the proxyInfo mbean property.
 *
 * @author Stefan Negrea
 */
@SuppressWarnings("rawtypes")
public class ContextDiscoveryComponent implements ResourceDiscoveryComponent<MBeanResourceComponent> {

    private final String JVM_ROUTE_PROPERTY = "jvmRoute";
    private final String PROXY_INFO_PROPERTY = "proxyInfo";
    private final String ADDITIONAL_CONFIG_BEAN_PROPERTY = "additionalConfigurationObjectName";

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MBeanResourceComponent> context) {
        String jvmRoute = this.getJvmRoute(context);
        String rawProxyInfo = this.getRawProxyInfo(context);

        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);
        Set<DiscoveredResourceDetails> entities = new HashSet<DiscoveredResourceDetails>();

        for (ProxyInfo.Context availableContext : proxyInfo.getAvailableContexts()) {
            if (availableContext.getJvmRoute().equals(jvmRoute)) {
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(),
                    availableContext.createKey(), availableContext.createName(), null, "mod_cluster Webapp Context",
                    null, null);
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
    private String getJvmRoute(ResourceDiscoveryContext<MBeanResourceComponent> context) {
        Configuration pluginConfig = context.getParentResourceComponent().getResourceContext().getPluginConfiguration();
        String objectName = pluginConfig.getSimple(ADDITIONAL_CONFIG_BEAN_PROPERTY).getStringValue();
        EmsBean engineBean = this.loadBean(objectName, context.getParentResourceComponent());

        return (String) engineBean.getAttribute(JVM_ROUTE_PROPERTY).refresh().toString();
    }

    /**
     * Retrieves raw proxy configuration from the parent context.
     * 
     * @param context the discovery context
     * @return raw proxy configuration
     */
    private String getRawProxyInfo(ResourceDiscoveryContext<MBeanResourceComponent> context) {
        EmsBean statsBean = context.getParentResourceComponent().getEmsBean();
        return (String) statsBean.getAttribute(PROXY_INFO_PROPERTY).refresh().toString();
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