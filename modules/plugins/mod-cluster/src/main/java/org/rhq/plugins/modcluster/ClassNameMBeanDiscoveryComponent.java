/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;

/**
 * Discovers mod_cluster contexts from the proxyInfo mbean property.
 *
 * @author Stefan Negrea
 */
public class ClassNameMBeanDiscoveryComponent extends MBeanResourceDiscoveryComponent<JMXComponent<?>> {

    private static final Log log = LogFactory.getLog(FileConfiguredMBeanResourceComponent.class);

    private static final String CLASS_NAME = "className";

    /* (non-Javadoc)
     * @see org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext, boolean)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent<?>> context,
        boolean skipUnknownProps) {

        Set<DiscoveredResourceDetails> results = super.discoverResources(context, skipUnknownProps);

        for (DiscoveredResourceDetails discoveredResource : results) {
            if (!isBeanConfiguredClassName(context, discoveredResource.getResourceKey())) {
                results.remove(discoveredResource);
            }
        }

        return results;
    }

    /**
     * @param context
     * @param objectName
     * @return
     */
    public boolean isBeanConfiguredClassName(ResourceDiscoveryContext<JMXComponent<?>> context, String objectName) {
        EmsBean emsBean = loadBean(context.getParentResourceComponent(), objectName);
        if (emsBean == null) {
            return false;
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(emsBean.getClass().getClassLoader());

            String className = context.getDefaultPluginConfiguration().getSimple(CLASS_NAME).getStringValue();

            return className.equals(emsBean.getClassTypeName());
        } catch (Exception e) {
            log.info(e);
            return false;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
