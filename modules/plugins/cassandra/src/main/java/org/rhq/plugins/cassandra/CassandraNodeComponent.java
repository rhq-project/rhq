/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.cassandra;

import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeComponent extends JMXServerComponent {

    private ResourceContext context;

    private EmsConnection emsConnection;

    @Override
    public void start(ResourceContext context) throws Exception {
        this.context = context;
    }

    @Override
    public void stop() {
        emsConnection = null;
    }

    @Override
    public AvailabilityType getAvailability() {
        if (emsConnection == null) {
            getEmsConnection();
        }
        return AvailabilityType.UP;
    }

    @Override
    public EmsConnection getEmsConnection() {
        if (emsConnection != null) {
            return emsConnection;
        }

        try {
            Configuration pluginConfig = context.getPluginConfiguration();

            ConnectionSettings connectionSettings = new ConnectionSettings();

            String connectionTypeDescriptorClass = pluginConfig.getSimple(JMXDiscoveryComponent.CONNECTION_TYPE)
                .getStringValue();
            PropertySimple serverUrl = pluginConfig
                .getSimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY);

            connectionSettings.initializeConnectionType((ConnectionTypeDescriptor) Class.forName(
                connectionTypeDescriptorClass).newInstance());
            // if not provided use the default serverUrl
            if (null != serverUrl) {
                connectionSettings.setServerUrl(serverUrl.getStringValue());
            }

            ConnectionFactory connectionFactory = new ConnectionFactory();
            ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);
            emsConnection = connectionProvider.connect();
            emsConnection.loadSynchronous(false);

            return emsConnection;
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to get EMS connection", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get EMS connection", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to get EMS connection", e);
        }
    }
}
