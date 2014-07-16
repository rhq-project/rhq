/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.plugins.storage;

import java.util.Set;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.jmx.EmbeddedJMXServerDiscoveryComponent;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * 
 * @author lzoubek@redhat.com
 *
 */
public class JvmServerDiscoveryComponent extends EmbeddedJMXServerDiscoveryComponent implements
    ResourceUpgradeFacet<ResourceComponent<?>> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context)
        throws Exception {
        Set<DiscoveredResourceDetails> details = super.discoverResources(context);
        if (!details.isEmpty()) {
            // singleton resource, so we can safely update first item
            DiscoveredResourceDetails res = details.iterator().next();
            res.setResourceName("JVM");
        }
        return details;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<ResourceComponent<?>> inventoriedResource) {
        ResourceUpgradeReport report = new ResourceUpgradeReport();
        if ("Cassandra Server JVM".equals(inventoriedResource.getName())) {
            report.setNewName("JVM");
            report.setNewDescription("The JVM of the Storage node");
            report.setForceGenericPropertyUpgrade(true);
            return report;
        }
        return null;
    }

}
