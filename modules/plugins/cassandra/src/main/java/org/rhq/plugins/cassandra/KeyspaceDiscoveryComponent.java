/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * @author John Sanda
 */
public class KeyspaceDiscoveryComponent implements ResourceDiscoveryComponent<JMXComponent<?>> {

    private static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        for (Object keyspaceName : getKeyspaces(context.getParentResourceComponent().getEmsConnection())) {
            details.add(new DiscoveredResourceDetails(context.getResourceType(), keyspaceName.toString(), keyspaceName
                .toString(), null, null, null, null));
        }

        return details;
    }

    /**
     * Retrieve the keyspace names from the StorageService bean.
     *
     * @param emsConnection Ems Connection
     * @return list of keyspaces
     */
    @SuppressWarnings("unchecked")
    private List<Object> getKeyspaces(EmsConnection emsConnection) {
        List<Object> value = null;

        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN, emsConnection);
        if (emsBean != null) {
            EmsAttribute attribute = emsBean.getAttribute("Keyspaces");
            if (attribute != null) {
                value = (List<Object>) attribute.refresh();
            }
        }

        if (value == null) {
            value = new ArrayList<Object>();
        }

        return value;
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     *
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    private EmsBean loadBean(String objectName, EmsConnection emsConnection) {
        if (emsConnection != null) {
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

        return null;
    }
}
