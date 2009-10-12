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

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discover JBossMessaging Destinations and change the description depending on plain JBM or ESB
 * @author Heiko W. Rupp
 *
 */
public class JBMDestinationDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {

        // Discover Queues and Topics via JMX
        Set<DiscoveredResourceDetails> results = super.discoverResources(context);

        // Now see if its plain JBM or ESB
        for (DiscoveredResourceDetails detail : results) {
            String name = detail.getResourceName();
            if (detail.getResourceKey().startsWith("jboss.esb")) {
                String descr = detail.getResourceDescription();
                descr = descr.replace("Messaging", " ESB");
                detail.setResourceDescription(descr);
                name = name + " (ESB)";
            } else {
                name = name + " (JBM)";
            }
            detail.setResourceName(name);
        }
        return results;
    }
}
