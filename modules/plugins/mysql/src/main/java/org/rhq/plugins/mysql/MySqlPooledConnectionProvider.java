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

package org.rhq.plugins.mysql;

import static org.rhq.plugins.mysql.MySqlDiscoveryComponent.CREDENTIALS_CONFIGURATION_PROPERTY;
import static org.rhq.plugins.mysql.MySqlDiscoveryComponent.PRINCIPAL_CONFIGURATION_PROPERTY;
import static org.rhq.plugins.mysql.MySqlDiscoveryComponent.buildConnectionURL;

import java.sql.Driver;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.database.BasePooledConnectionProvider;

/**
 * A MySql plugin adapted {@link org.rhq.plugins.database.PooledConnectionProvider}.
 *
 * @author Thomas Segismont
 */
public class MySqlPooledConnectionProvider extends BasePooledConnectionProvider {

    public MySqlPooledConnectionProvider(Configuration pluginConfig) throws Exception {
        super(pluginConfig);
    }

    @Override
    protected Class<Driver> getDriverClass() throws ClassNotFoundException {
        return (Class<Driver>) Class.forName("com.mysql.jdbc.Driver");
    }

    @Override
    protected String getJdbcUrl() {
        return buildConnectionURL(pluginConfig);
    }

    @Override
    protected String getUsername() {
        return pluginConfig.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
    }

    @Override
    protected String getPassword() {
        return pluginConfig.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();
    }
}
