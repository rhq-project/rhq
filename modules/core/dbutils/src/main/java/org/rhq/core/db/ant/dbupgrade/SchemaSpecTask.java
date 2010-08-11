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
package org.rhq.core.db.ant.dbupgrade;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import mazz.i18n.Msg;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.TypeMap;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * A task that performs an individual step to help upgrade a schema to a particular version.
 */
public abstract class SchemaSpecTask extends Task {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private Connection connection = null;
    private DBUpgrader upgrader = null;

    // each schema spec can be targeted to a specific database vendor and a specific DB version
    private String targetDBVendor;
    private String targetDBVersion;

    private String ignoreError;

    // cache all known JDBC SQL data types
    private static Map<String, Integer> SQL_TYPES = new HashMap<String, Integer>();

    static {
        Field[] fields = Types.class.getDeclaredFields();

        for (Field field : fields) {
            int mods = field.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)) {
                try {
                    SQL_TYPES.put(field.getName(), new Integer(field.getInt(null)));
                } catch (IllegalAccessException iae) {
                    throw new IllegalStateException(iae);
                }
            }
        }
    }

    /**
     * Sets the vendor name of the target DB where this SQL should run. Any other DB should not have this SQL executed
     * on it.
     *
     * @param vendor database vendor
     */
    public void setTargetDBVendor(String vendor) {
        targetDBVendor = vendor;
    }

    /**
     * Sets the version string of the target DB where this SQL should run. Any other DB version should not have this SQL
     * executed on it.
     *
     * @param version database version
     */
    public void setTargetDBVersion(String version) {
        targetDBVersion = version;
    }

    /**
     * Sets the vendor name of the target DB where this SQL should run. Any other DB should not have this SQL executed
     * on it.
     *
     * @return vendor string
     */
    public String getTargetDBVendor() {
        return targetDBVendor;
    }

    /**
     * Gets the version string of the target DB where this SQL should run. Any other DB version should not have this SQL
     * executed on it.
     *
     * @return version string
     */
    public String getTargetDBVersion() {
        return targetDBVersion;
    }

    public String getIgnoreError() {
        return ignoreError;
    }

    public void setIgnoreError(String ignoreError) {
        this.ignoreError = ignoreError;
    }

    public boolean isIgnoreError() {
        return new Boolean(this.ignoreError);
    }

    /**
     * Returns <code>true</code> if this task's database is targeted for this schema spec. Note that if
     * {@link #getTargetDBVendor()} is not specified, then any specified {@link #getTargetDBVersion()} will cause a
     * build exception to occur. You cannot specific a version string without indicating which vendor's database you are
     * talking about.
     *
     * @return <code>true</code> if this schema spec task should run; <code>false</code> if this task's database is not
     *         targeted and thus this task should not execute
     *
     * @see    #getTargetDBVendor()
     * @see    #getTargetDBVersion()
     */
    public boolean isDBTargeted() {
        if ((targetDBVersion != null) && (targetDBVendor == null)) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_TARGET_VERSION_WITHOUT_VENDOR,
                getTaskName(), targetDBVersion));
        }

        if (targetDBVendor != null) {
            DatabaseType db_type = getDatabaseType();

            if (!targetDBVendor.equalsIgnoreCase(db_type.getVendor())) {
                log(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_VENDOR_MISMATCH, targetDBVendor, db_type
                    .getVendor()));
                return false;
            }

            if ((targetDBVersion != null) && !targetDBVersion.equalsIgnoreCase(db_type.getVersion())) {
                log(MSG.getMsg(DbAntI18NResourceKeys.SCHEMA_SPEC_TASK_VERSION_MISMATCH, targetDBVendor,
                    targetDBVersion, db_type.getVersion()));
                return false;
            }
        }

        return true;
    }

    /**
     * Initializes this task with a database connection and a {@link DBUpgrader} object.
     *
     * @param db_conn
     * @param db_upgrader
     */
    public void initialize(Connection db_conn, DBUpgrader db_upgrader) {
        this.connection = db_conn;
        this.upgrader = db_upgrader;
    }

    /**
     * Returns a database connection this task can use.
     *
     * @return db connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Get a new connection from this task's owning {@link DBUpgrader} object.
     *
     * @return db connection
     *
     * @throws SQLException
     */
    public Connection getNewConnection() throws SQLException {
        return upgrader.getConnection();
    }

    /**
     * Returns the type of database being upgraded.
     *
     * @return database type
     */
    public DatabaseType getDatabaseType() {
        return upgrader.getDatabaseType();
    }

    /**
     * Given a generic type name, this returns the {@link java.sql.Types} integer value of that type. In effect, it maps
     * the generic type name with the JDBC SQL type ID.
     *
     * @param  generic_type_name
     *
     * @return the JDBC SQL type ID of the given generic type
     *
     * @throws BuildException if the type is not a known or supported type
     */
    public int translateSqlType(String generic_type_name) throws BuildException {
        String mapped_type = TypeMap.getMappedType(upgrader.getTypeMaps(), generic_type_name, null);
        Integer sql_type = SQL_TYPES.get(mapped_type);

        if (sql_type == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.INVALID_JDBC_SQL_TYPE, generic_type_name));
        }

        return sql_type.intValue();
    }

    /**
     * Returns the database-specific type name for the given generic type.
     *
     * @param  generic_type_name
     *
     * @return the database specific type name that correlates to the generic type name
     *
     * @throws BuildException no database specific type name was defined
     */
    public String getDBSpecificTypeName(String generic_type_name) throws BuildException {
        String mapped_type = TypeMap.getMappedType(TypeMap.loadKnownTypeMaps(), generic_type_name, getDatabaseType());

        if (mapped_type == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.NO_DB_SPECIFIC_TYPE_MAPPING, generic_type_name,
                getDatabaseType()));
        }

        return mapped_type;
    }
}