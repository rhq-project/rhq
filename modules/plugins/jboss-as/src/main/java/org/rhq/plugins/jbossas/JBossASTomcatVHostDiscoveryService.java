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

package org.rhq.plugins.jbossas;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discover virtual hosts
 * @author Heiko W. Rupp
 *
 */
public class JBossASTomcatVHostDiscoveryService extends MBeanResourceDiscoveryComponent<JMXComponent> {

    static Pattern pattern = Pattern.compile(".*host=([\\w.]+).*");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> discoveryContext) {

        Set<DiscoveredResourceDetails> resources = super.discoverResources(discoveryContext);

        for (DiscoveredResourceDetails detail : resources) {
            String name = detail.getResourceName();
            Matcher m = pattern.matcher(name);
            if (m.matches()) {
                name = m.group(1);
            }
            detail.setResourceName(name);
            detail.setResourceDescription("Virtual Host " + name);
        }
        return resources;
    }
}
