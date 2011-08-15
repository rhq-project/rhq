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

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.modcluster.helper.JBossHelper;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "rawtypes" })
public class ModclusterServerComponent extends MBeanResourceComponent {

    private final static String JBOSS_SERVER_HOME_DIR = "jboss.server.home.dir";

    @SuppressWarnings({ "unchecked", "deprecation", "static-access" })
    @Override
    public void start(ResourceContext context) {
        super.start(context);

        String serverHomeDirectory = null;
        if (this.resourceContext.getParentResourceComponent() instanceof ApplicationServerComponent) {
            ApplicationServerComponent parentComponent = (ApplicationServerComponent) this.resourceContext
                .getParentResourceComponent();

            serverHomeDirectory = parentComponent.getResourceContext().getPluginConfiguration()
                .getSimple("serverHomeDir").getStringValue();

        } else if (this.resourceContext.getParentResourceComponent() instanceof JBossASServerComponent) {
            JBossASServerComponent parentComponent = (JBossASServerComponent) this.resourceContext
                .getParentResourceComponent();

            serverHomeDirectory = parentComponent.getPluginConfiguration()
                .getSimple(parentComponent.JBOSS_HOME_DIR_CONFIG_PROP).getStringValue();
        }

        if (serverHomeDirectory != null) {
            PropertySimple serverHomeDirectoryProperty = null;
            serverHomeDirectoryProperty = new PropertySimple();
            serverHomeDirectoryProperty.setName(JBOSS_SERVER_HOME_DIR);
            serverHomeDirectoryProperty.setStringValue(serverHomeDirectory);
            this.resourceContext.getPluginConfiguration().put(serverHomeDirectoryProperty);
        }
    };

    //private final static String PROXY_INFO_PROPERTY_NAME

    /* (non-Javadoc)
     * @see org.rhq.plugins.jmx.MBeanResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        String rawProxyInfo = JBossHelper.getRawProxyInfo(getEmsBean());

        if (rawProxyInfo == null) {
            return AvailabilityType.DOWN;
        }

        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);
        if (proxyInfo.getAvailableNodes().size() == 0) {
            return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }
}
