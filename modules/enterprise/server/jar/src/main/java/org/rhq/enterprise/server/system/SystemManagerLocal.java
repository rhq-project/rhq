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
     * Run an analyze command.
     *
     * @param whoami the user requesting the operation
     */
    long analyze(Subject whoami);

    /**
     * Run database-specific cleanup routines -- on PostgreSQL we do a VACUUM ANALYZE. On other databases we just return
     * -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami);

    /**
     * Run database-specific cleanup routines for a bunch of tables -- on PostgreSQL we do a VACUUM ANALYZE. On other
     * databases we just return -1.
     *
     * @param  whoami     the user requesting the operation
     * @param  tableNames names of specific tables
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuum(Subject whoami, String[] tableNames);

    /**
     * Run database-specific cleanup routines on appdef tables -- on PostgreSQL we do a VACUUM ANALYZE against the
     * relevant appdef, authz and measurement tables. On other databases we just return -1.
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long vacuumAppdef(Subject whoami);

    /**
     * Run a REINDEX command on all HQ data tables
     *
     * @param  whoami the user requesting the operation
     *
     * @return The time it took to vaccum, in milliseconds, or -1 if the database is not PostgreSQL.
     */
    long reindex(Subject whoami);

    /**
     * Get the "root" server configuration, that means those keys that have the NULL prefix.
     *
     * @return Properties
     */
    Properties getSystemConfiguration();

    /**
     * Set the server configuration
     *
     * @param subject    the user who wants to change the settings
     * @param properties
     */
    void setSystemConfiguration(Subject subject, Properties properties);

    /**
     * Load the hibernate statistics mbean
     */
    void enableHibernateStatistics();

    boolean isMonitoringEnabled();

    License getLicense();

    void updateLicense(Subject subject, byte[] licenseData);

    Date getExpiration();

    void reconfigureSystem(Subject whoami);
}