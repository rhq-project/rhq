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

import static org.rhq.plugins.database.DatabasePluginUtil.hasConnectionPoolingSupport;

import java.sql.Connection;

import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Base class for nested database components.
 * @author Greg Hinkle
 */
public abstract class AbstractDatabaseComponent<T extends DatabaseComponent<?>> implements DatabaseComponent,
    ConnectionPoolingSupport {

    private PooledConnectionProvider pooledConnectionProvider;
    protected ResourceContext<T> resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        if (hasConnectionPoolingSupport(resourceContext.getParentResourceComponent())) {
            pooledConnectionProvider = ((ConnectionPoolingSupport) resourceContext.getParentResourceComponent())
                .getPooledConnectionProvider();
        }
    }

    public void stop() {
        pooledConnectionProvider = null;
    }

    @Override
    public boolean supportsConnectionPooling() {
        return pooledConnectionProvider != null;
    }

    @Override
    public PooledConnectionProvider getPooledConnectionProvider() {
        return pooledConnectionProvider;
    }

    public Connection getConnection() {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }

    public void removeConnection() {
        this.resourceContext.getParentResourceComponent().removeConnection();
    }

    @Override
    public String toString() {
        return getClass().getName() + " key=" + resourceContext.getResourceKey();
    }
}
