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

package org.jboss.on.plugins.tomcat;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discover Application Cache
 * 
 * @author Jay Shaughnessy
 * @author Heiko W. Rupp
 *
 */
public class TomcatCacheDiscoveryComponent extends MBeanResourceDiscoveryComponent<TomcatVHostComponent> {

    static Pattern hostPattern = Pattern.compile(".*host=([\\w.]+).*");
    static Pattern pathPattern = Pattern.compile(".*path=([\\w.]+).*");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<TomcatVHostComponent> discoveryContext) {

        Set<DiscoveredResourceDetails> resources = super.discoverResources(discoveryContext);
        Configuration pluginConfiguration = discoveryContext.getDefaultPluginConfiguration();

        for (DiscoveredResourceDetails detail : resources) {
            String name = detail.getResourceName();
            Matcher m = hostPattern.matcher(name);
            String host = null;
            String path = null;
            if (m.matches()) {
                host = m.group(1);
                pluginConfiguration.put(new PropertySimple(TomcatCacheComponent.PROPERTY_HOST, host));
            }
            m = pathPattern.matcher(name);
            if (m.matches()) {
                path = m.group(1);
                pluginConfiguration.put(new PropertySimple(TomcatCacheComponent.PROPERTY_PATH, path));
            }

            if ((null != host) && (null != path)) {
                name = host + path;
                detail.setResourceName(name);
            }
        }
        return resources;
    }
}
