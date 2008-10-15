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
package org.rhq.plugins.hibernate;

import java.util.HashSet;
import java.util.Set;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Discovers hibernate entities from a hibernate stats mbean
 *
 * @author Greg Hinkle
 */
public class EntityDiscoveryComponent implements ResourceDiscoveryComponent<MBeanResourceComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MBeanResourceComponent> context) {
        Set<DiscoveredResourceDetails> entities = new HashSet<DiscoveredResourceDetails>();

        EmsBean statsBean = context.getParentResourceComponent().getEmsBean();

        String[] entityNames = (String[]) statsBean.getAttribute("EntityNames").refresh();

        for (String entityName : entityNames) {
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), entityName,
                entityName, null, "Hibernate Entity", null, null);
            entities.add(detail);
        }

        return entities;
    }
}