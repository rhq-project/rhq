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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * Abstract base class to discover JBossMessaging and JBossMQ related stuff
 * @author Heiko W. Rupp
 */
public abstract class AbstractMessagingDiscoveryComponent extends MBeanResourceDiscoveryComponent {

    protected Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context,
        String objectName, String resourceName, String resourceDescription) {

        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectName);
        JMXComponent parentResourceComponent = context.getParentResourceComponent();
        EmsConnection connection = parentResourceComponent.getEmsConnection();

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());
        if (beans.size() == 1) {
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), objectName,
                resourceName, "", resourceDescription, pluginConfig, null);
            result.add(detail);
        }
        return result;

    }
}