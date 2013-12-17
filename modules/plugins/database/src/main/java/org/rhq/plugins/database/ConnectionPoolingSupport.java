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

/**
 * <p>
 * A contract that all database related resource components should obey to.
 * </p>
 * <p>
 * Results of calls to {@link #supportsConnectionPooling()} and {@link #getPooledConnectionProvider()}
 * <strong>MUST</strong> be consistent.
 * </p>
 * <p>
 * In practice, a top level server database component should be able to create a {@link PooledConnectionProvider}
 * instance, and child servers and services should indicate they support connection pooling only if their parent
 * component does.
 * </p>
 *
 * @author Thomas Segismont
 */
public interface ConnectionPoolingSupport {

    /**
     * @return true if this component can give a reference to a {@link PooledConnectionProvider}, false otherwise.
     */
    boolean supportsConnectionPooling();

    /**
     * @return a reference to a {@link PooledConnectionProvider} or null if this component does not support connection
     * pooling.
     * @see #supportsConnectionPooling()
     */
    PooledConnectionProvider getPooledConnectionProvider();

}
