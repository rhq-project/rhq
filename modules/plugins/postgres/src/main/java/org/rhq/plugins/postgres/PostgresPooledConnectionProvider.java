/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.plugins.postgres;

import static org.rhq.plugins.postgres.PostgresDiscoveryComponent.CREDENTIALS_CONFIGURATION_PROPERTY;
import static org.rhq.plugins.postgres.PostgresDiscoveryComponent.DRIVER_CONFIGURATION_PROPERTY;
import static org.rhq.plugins.postgres.PostgresDiscoveryComponent.PRINCIPAL_CONFIGURATION_PROPERTY;
import static org.rhq.plugins.postgres.PostgresDiscoveryComponent.buildUrl;

import java.sql.Driver;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.database.BasePooledConnectionProvider;

/**
 * A Postgres plugin adapated {@link org.rhq.plugins.database.PooledConnectionProvider}
 * 
 * @author Thomas Segismont
 */
public class PostgresPooledConnectionProvider extends BasePooledConnectionProvider {

    public PostgresPooledConnectionProvider(Configuration pluginConfig) throws Exception {
        super(pluginConfig);
    }

    @Override
    protected Class<Driver> getDriverClass() throws ClassNotFoundException {
        return (Class<Driver>) Class.forName(pluginConfig.getSimple(DRIVER_CONFIGURATION_PROPERTY).getStringValue());
    }

    @Override
    protected String getJdbcUrl() {
        return buildUrl(pluginConfig);
    }

    @Override
    protected String getPassword() {
        return pluginConfig.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();
    }

    @Override
    protected String getUsername() {
        return pluginConfig.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
    }
}
