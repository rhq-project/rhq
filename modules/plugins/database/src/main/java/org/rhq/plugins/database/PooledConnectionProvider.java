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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for resource components providing pooled {@link Connection} objects.
 *
 * @author Thomas Segismont
 */
public interface PooledConnectionProvider {
    /**
     * Get a pooled connection. It's the responsibility of the caller to return the connection to the pool by calling
     * {@link java.sql.Connection#close()} on the returned {@link Connection} object.
     *
     * @return a pooled {@link Connection}
     * @throws SQLException if a pooled connection could not be retrieved
     */
    Connection getPooledConnection() throws SQLException;
}
