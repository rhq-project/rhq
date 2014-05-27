/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.jmx;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.PluginConfigProps.BEANS_QUERY_STRING;
import static org.rhq.modules.plugins.jbossas7.jmx.ApplicationMBeansDiscoveryComponent.loadEmsConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * A component class representing a container for appplication MBeans resources.
 * 
 * @author Thomas Segismont
 * @see ApplicationMBeansDiscoveryComponent
 */
public class ApplicationMBeansComponent implements JMXComponent<BaseComponent<?>> {
    private static final Log LOG = LogFactory.getLog(ApplicationMBeansComponent.class);

    private ResourceContext resourceContext;
    private Configuration pluginConfig;
    private EmsConnection emsConnection;
    private String beansQueryString;

    @Override
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        pluginConfig = resourceContext.getPluginConfiguration();
        beansQueryString = pluginConfig.getSimpleValue(BEANS_QUERY_STRING);
        emsConnection = loadEmsConnection(pluginConfig, resourceContext.getTemporaryDirectory());
    }

    @Override
    public void stop() {
        pluginConfig = null;
        if (emsConnection != null) {
            emsConnection.close();
        }
    }

    @Override
    public EmsConnection getEmsConnection() {
        synchronized (this) {
            if (emsConnection == null || !emsConnection.getConnectionProvider().isConnected()) {
                emsConnection = loadEmsConnection(pluginConfig, resourceContext.getTemporaryDirectory());
            }
        }
        return emsConnection;
    }

    @Override
    public AvailabilityType getAvailability() {
        EmsConnection connection = getEmsConnection();
        if (connection == null) {
            return DOWN;
        }
        if (!hasApplicationMBeans()) {
            LOG.warn("Found no MBeans with query '" + beansQueryString + "'");
            return DOWN;
        }
        return UP;
    }

    /**
     * Checks if application MBeans are available. The default implementation uses the EMS query string stored in the
     * resource plugin config.
     * 
     * @return true if MBeans are available
     */
    protected boolean hasApplicationMBeans() {
        return !emsConnection.queryBeans(beansQueryString).isEmpty();
    }
}
