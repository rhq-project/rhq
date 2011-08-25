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

package org.rhq.plugins.modcluster;

import java.util.Set;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings("rawtypes")
public class LoadMetricDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context,
        boolean skipUnknownProps) {

        Set<DiscoveredResourceDetails> results = super.discoverResources(context, skipUnknownProps);

        for (DiscoveredResourceDetails discoveredResource : results) {

            String name = getBeanConfiguredClassName(context, discoveredResource.getResourceKey());

            name = name.substring(name.lastIndexOf(".") + 1);
            name = name.substring(0, name.lastIndexOf("LoadMetric"));

            discoveredResource.setResourceName(name);
            discoveredResource.setResourceDescription("Load Metric");
        }

        return results;
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     * 
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    protected EmsBean loadBean(EmsConnection emsConnection, String objectName) {
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

    /**
     * @param context
     * @param objectName
     * @return
     */
    private String getBeanConfiguredClassName(ResourceDiscoveryContext<JMXComponent> context, String objectName) {
        EmsConnection connection = context.getParentResourceComponent().getEmsConnection();
        EmsBean emsBean = loadBean(connection, objectName);

        return emsBean.getClassTypeName();
    }
}
