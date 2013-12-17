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

package org.rhq.plugins.database;

import static org.rhq.plugins.database.H2Database.DRIVER_CLASS_PROPERTY;
import static org.rhq.plugins.database.H2Database.PASSWORD_PROPERTY;
import static org.rhq.plugins.database.H2Database.URL_PROPERTY;
import static org.rhq.plugins.database.H2Database.USERNAME_PROPERTY;

import java.sql.Driver;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Thomas Segismont
 */
class H2PooledConnectionProvider extends BasePooledConnectionProvider {

    public H2PooledConnectionProvider(Configuration pluginConfig) throws Exception {
        super(pluginConfig);
    }

    @Override
    protected Class<Driver> getDriverClass() throws ClassNotFoundException {
        return (Class<Driver>) Class.forName(pluginConfig.getSimpleValue(DRIVER_CLASS_PROPERTY, "org.h2.Driver"));
    }

    @Override
    protected String getJdbcUrl() {
        return pluginConfig.getSimpleValue(URL_PROPERTY, "jdbc:h2:test");
    }

    @Override
    protected String getUsername() {
        return pluginConfig.getSimple(USERNAME_PROPERTY).getStringValue();
    }

    @Override
    protected String getPassword() {
        return pluginConfig.getSimple(PASSWORD_PROPERTY).getStringValue();
    }
}
