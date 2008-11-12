/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.system;

import java.util.Date;
import java.util.Properties;

import javax.ejb.Local;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.license.License;

/**
 * Provides access to the server cloud's system configuration as well as some methods
 * to perform configuration on the server in which this bean is running.
 */
@Local
public interface SystemManagerLocal {
    /**
     * Returns the {@link DatabaseType} that corresponds to the database the JON Server uses for its backend.
     *
     * <p>This method is mainly to allow the caller to determine the kind of database in use so as to determine what
     * syntax to use for a particular native query.</p>
     *
     * @return the type of database
     */
    DatabaseType getDatabaseType();

    /**
     * Get the server cloud configuration. These are the server configurations that will be
     * the same for all servers in the HA server cloud.
     *
     * @return Properties
     */
    Properties getSystemConfiguration();

    /**
     * Set the server cloud configuration.  The given properties will be the new settings
     * for all servers in the HA server cloud.
     *
     * @param subject    the user who wants to change the settings
     * @param properties the new system configuration settings
     */
    void setSystemConfiguration(Subject subject, Properties properties);

    /**
     * Creates and registers the Hibernate Statistics MBean. This allows us to monitor
     * our own Hibernate usage.
     */
    void enableHibernateStatistics();

    /**
     * Performs some reconfiguration things on the server where we are running.
     * This includes redeploying the configured JAAS modules.
     */
    void reconfigureSystem(Subject whoami);

    /**
     * Run analyze command on PostgreSQL databases. On non-PostgreSQL, this returns -1.
     *
     * @param whoami the user requesting the operation
     *
     * @return The time it took to analyze, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long analyze(Subject whoami);

    /**
     * Reindexes all tables that need to be periodically reindexed.
     * For Oracle, this "rebuilds" the indexes, for PostgreSQL, its a "reindex". 
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to reindex, in milliseconds
     */
    long reindex(Subject whoami);

    /**
     * Run database-specific cleanup routines.
     * On PostgreSQL we do a VACUUM ANALYZE on all tables. On other databases we just return -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami);

    /**
     * Run database-specific cleanup routines for the given tables.
     * On PostgreSQL we do a VACUUM ANALYZE on the given tables. On other databases we just return -1.
     *
     * @param  whoami     the user requesting the operation
     * @param  tableNames names of specific tables that will be vacuumed.
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami, String[] tableNames);

    /**
     * Run database-specific cleanup routines on appdef tables.
     * On PostgreSQL we do a VACUUM ANALYZE against the relevant tables.  On other databases we just return -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuumAppdef(Subject whoami);

    //////////////////////////////////
    // license specific methods follow

    boolean isMonitoringEnabled();

    License getLicense();

    void updateLicense(Subject subject, byte[] licenseData);

    Date getExpiration();
}