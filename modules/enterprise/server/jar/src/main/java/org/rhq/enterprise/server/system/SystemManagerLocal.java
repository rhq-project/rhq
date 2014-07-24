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

import java.util.Properties;

import javax.ejb.Local;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;

/**
 * Provides access to the server cloud's system configuration as well as some methods
 * to perform configuration on the server in which this bean is running.
 */
@Local
public interface SystemManagerLocal extends SystemManagerRemote {
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
     * Schedules the internal timer job that periodically refreshes the configuration cache.
     * This is needed in case a user changed the system configuration on another server in the HA
     * cloud - this config cache reloader will load in that new configuration.
     */
    void scheduleConfigCacheReloader();

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

    /**
     * Ensures the installer has been undeployed. Installer must be undeployed
     * to ensure the server deployment is secure.
     */
    @Deprecated
    void undeployInstaller();

    /**
     * Grabs the current system configuration from the database and reloads the cache with it.
     * This is meant for internal use only! You probably want to use {@link #getSystemConfiguration()}
     * instead.
     */
    void loadSystemConfigurationCacheInNewTx();

    /**
     * Grabs the current system configuration from the database and reloads the cache with it.
     * This is meant for internal use only! You probably want to use {@link #getSystemConfiguration()}
     * instead.
     */
    void loadSystemConfigurationCache();

    boolean isDebugModeEnabled();

    boolean isExperimentalFeaturesEnabled();

    boolean isLdapAuthorizationEnabled();

    void validateSystemConfiguration(Subject subject, Properties properties) throws InvalidSystemConfigurationException;

    void dumpSystemInfo(Subject subject);

    /**
     * The storage cluster settings are stored as read-only system settings. They should be updated through the storage
     * subsystem. This API is provided for use ONLY by the storage subsystem.
     *
     * @param subject The user who wants to change the settings
     * @param settings The new storage cluster settings
     * @throws IllegalArgumentException If the settings contain anything other than storage cluster settings.
     */
    void setStorageClusterSettings(Subject subject, SystemSettings settings);

    /**
     * The {@link SystemManagerRemote#getSystemSettings(org.rhq.core.domain.auth.Subject)} returns the system settings
     * with all the password fields masked so that remote clients cannot get hold of the passwords in it. It also excludes
     * non-public system settings from the results.
     * <p/>
     * If the password or a non-public setting is needed inside some SLSB, use this method to obtain the system settings
     * with all the fields and the passwords in clear text.
     * <p />
     * Note that the returned instance MUST NOT leak out of the RHQ server.
     *
     * @return the system settings with the password fields in clear.
     * @param includePrivateSettings whether or not to include the private system settings (i.e.
     * {@link org.rhq.core.domain.common.composite.SystemSetting#isPublic()} returns false)
     */
    SystemSettings getUnmaskedSystemSettings(boolean includePrivateSettings);

    /**
     * The {@link #getUnmaskedSystemSettings(boolean)} returns the system settings with all the passwords in clear text.
     * If you want to obtain an instance where the passwords would neither be clear text, nor would they be masked away,
     * you can use this method, that will return the system settings with the passwords obfuscated.
     * <p/>
     * Note that such instance <b>CANNOT</b> be passed to {@link #setSystemSettings(org.rhq.core.domain.auth.Subject,
       org.rhq.core.domain.common.composite.SystemSettings)}!!! Storing the obfuscated passwords would obfuscate them
     * again!!! To be able to persist an instance, you first need to
     * {@link #deobfuscate(org.rhq.core.domain.common.composite.SystemSettings) deobfuscate} it.
     * <p />
     * Note that the returned instance MUST NOT leak out of the RHQ server. It contains the passwords in a decodable
     * form and also non-public system settings.
     *
     * @param includePrivateSettings whether or not to include the private system settings (i.e.
     * {@link org.rhq.core.domain.common.composite.SystemSetting#isPublic()} returns false)
     *
     * @return an instance of system settings with all the passwords obfuscated.
     */
    SystemSettings getObfuscatedSystemSettings(boolean includePrivateSettings);

    void deobfuscate(SystemSettings systemSettings);

    /**
     * Internal use only.  Sets any setting (other than LAST_SYSTEM_CONFIG_UPDATE_TIME) regardless of whether
     * it is private or read-only.  Guarantees proper cache update.  Performs no validation or un/masking.
     *
     * @param setting
     * @param value
     */
    void setAnySystemSetting(SystemSetting setting, String value);

    /**
     * Internal use only.  Like {@link SystemManagerRemote#setSystemSettings(Subject, SystemSettings)} but can
     * bypass validation and also ignore the readOnly constraint.
     *
     * @param settings
     * @param skipValidation if true, skip validation
     * @param ignoreReadOnly if true, ignore the readOnly constraint and set new values if supplied
     * @param value
     */
    void setAnySystemSettings(SystemSettings settings, boolean skipValidation, boolean ignoreReadOnly);

}
