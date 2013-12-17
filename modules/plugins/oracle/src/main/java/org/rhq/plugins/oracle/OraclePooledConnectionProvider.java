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

package org.rhq.plugins.oracle;

import java.sql.Driver;
import java.util.Properties;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.database.BasePooledConnectionProvider;

/**
 * An Oracle plugin adapted {@link org.rhq.plugins.database.PooledConnectionProvider}.
 *
 * @author Thomas Segismont
 */
public class OraclePooledConnectionProvider extends BasePooledConnectionProvider {

    static final String DRIVER_CLASS_PROPERTY = "driverClass";
    static final String PRINCIPAL_PROPERTY = "principal";
    static final String CREDENTIALS_PROPERTY = "credentials";

    public OraclePooledConnectionProvider(Configuration pluginConfig) throws Exception {
        super(pluginConfig);
    }

    @Override
    protected Class<Driver> getDriverClass() throws ClassNotFoundException {
        return (Class<Driver>) Class.forName(pluginConfig.getSimple(DRIVER_CLASS_PROPERTY).getStringValue());
    }

    @Override
    protected String getJdbcUrl() {
        return OracleServerComponent.buildUrl(pluginConfig);
    }

    @Override
    protected String getUsername() {
        return pluginConfig.getSimple(PRINCIPAL_PROPERTY).getStringValue();
    }

    @Override
    protected String getPassword() {
        return pluginConfig.getSimple(CREDENTIALS_PROPERTY).getStringValue();
    }

    @Override
    protected Properties getConnectionProperties() {
        Properties connectionProperties = super.getConnectionProperties();
        if (getUsername().equalsIgnoreCase("SYS")) {
            connectionProperties.put("internal_logon", "sysdba");
        }
        return connectionProperties;
    }
}
