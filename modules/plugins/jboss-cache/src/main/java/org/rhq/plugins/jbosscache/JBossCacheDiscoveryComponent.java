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

package org.rhq.plugins.jbosscache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discover JBossCache instances. The only way to detect them are to
 * look for "*:cache-interceptor=CacheMgmtInterceptor,*" or "*:treecache-interceptor=CacheMgmtInterceptor,*"
 * This is done in {@link MBeanResourceDiscoveryComponent}. We postprocess the result here to
 * get the base MBean name (without the property for detection) to be able to use this later and also
 * for display purposes, as this is the MBean name the user set up in his MBean.
 *
 * @author Heiko W. Rupp
 */
public class JBossCacheDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent<?>> {

    private static final Log log = LogFactory.getLog(JBossCacheDiscoveryComponent.class);

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context) {

        ResourceContext parentCtx = context.getParentResourceContext();

        if (!(parentCtx.getParentResourceComponent() instanceof JMXComponent)) {
            return Collections.emptySet();
        }

        JMXComponent<JBossASServerComponent<?>> gparentComponent = (JMXComponent<JBossASServerComponent<?>>) parentCtx.getParentResourceComponent();

        Set<DiscoveredResourceDetails> discovered = super.performDiscovery(context.getDefaultPluginConfiguration(), gparentComponent, context.getResourceType(), false);

        Set<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>(discovered.size());

        // Normalize the base object names from the key
        for (DiscoveredResourceDetails detail : discovered) {
            boolean isTreeCache = false;
            String key = detail.getResourceKey();
            if (key.contains("treecache-interceptor="))
                isTreeCache = true;

            try {
                ObjectName on = ObjectName.getInstance(key);
                key = on.getDomain();
                key += ":";
                Set<String> propKeys = on.getKeyPropertyList().keySet();
                for (String prop : propKeys) {
                    if (!(prop.contains("cache"))) {
                        key += prop + "=" + on.getKeyProperty(prop);
                        key += ",";
                    }
                }
                if (key.endsWith(","))
                    key = key.substring(0, key.length() - 1);
                if (log.isDebugEnabled())
                    log.debug("Translated " + detail.getResourceKey() + " to " + key);
                detail.setResourceKey(key);
                detail.setResourceName(key);
                String descr = "";
                if (isTreeCache)
                    descr = "Tree";
                descr += "Cache at " + key;
                detail.setResourceDescription(descr);

                Configuration pluginConfiguration = detail.getPluginConfiguration();
                PropertySimple onProp = pluginConfiguration.getSimple("objectName");
                onProp.setStringValue(key);

                PropertySimple isTC = new PropertySimple("isTreeCache", isTreeCache);
                pluginConfiguration.put(isTC);

                results.add(detail);
            } catch (MalformedObjectNameException e) {
                log.warn("Invalid obectname : " + key);
            }
        }

        return results;
    }
}
