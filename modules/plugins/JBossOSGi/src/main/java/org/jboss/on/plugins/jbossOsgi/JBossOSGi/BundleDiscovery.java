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
package org.jboss.on.plugins.jbossOsgi.JBossOSGi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * Discovery Class for OSGi Bundles
 *
 * @author Heiko W. Rupp
 */
public class BundleDiscovery implements ResourceDiscoveryComponent<JBossOsgiServerComponent> {

    private final Log log = LogFactory.getLog(BundleDiscovery.class);

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<JBossOsgiServerComponent> ctx) throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        String objectNamePattern="jboss.osgi:bundle=%name%,id=%id%";

        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectNamePattern);

        EmsConnection conn = ctx.getParentResourceComponent().getEmsConnection(ctx.getParentResourceContext().getPluginConfiguration());
        List<EmsBean> beans = conn.queryBeans(queryUtility.getTranslatedQuery());

        for (EmsBean bean: beans) {

            queryUtility.setMatchedKeyValues(bean.getBeanName().getKeyProperties());
            //Skip unknown beans
            if (queryUtility.isContainsExtraKeyProperties(bean.getBeanName().getKeyProperties().keySet()))
                continue;

            String resourceKey = bean.getBeanName().getCanonicalName(); // The detected object name
            String name = queryUtility.formatMessage("{name}");

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(ctx.getResourceType(),
                    resourceKey,
                    name,
                    null,
                    "An OSGiBundle",
                    null,
                    null);
            details.add(detail);

        }

        log.info("Discovered " + details.size() +" bundles");

        return details;
    }
}
