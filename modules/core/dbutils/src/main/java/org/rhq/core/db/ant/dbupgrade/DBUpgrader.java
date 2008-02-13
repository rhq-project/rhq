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

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.TypeMap;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * ANT task that performs database upgrades of an existing schema.
 */
public class DBUpgrader extends Task {
    private static final Msg MSG = DbAntI18NFactory.getMsg();
    private static final String SCHEMA_MOD_IN_PROGRESS = " *** UPGRADE IN PROGRESS: migrating to version ";

    private List<SchemaSpec> schemaSpecs = new ArrayList<SchemaSpec>();

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    // The query to find the existing schema version uses these.
    // It is of the form: SELECT valueColumn FROM tableName WHERE keyColumn = keyMatch
    private String valueColumn;
    private String tableName;
    private String keyColumn;
    private String keyMatch;

    private File typeMapFile;
    private Collection<TypeMap> typeMaps;

    private String startSchemaVersionString;
    private SchemaVersion startSchemaVersion;
    private String targetSchemaVersionString;
    private SchemaVersion targetSchemaVersion;

    private DatabaseType databaseType;

    /**
     * The URL to the database that is to be upgraded.
     *
     * @param jdbcUrl
     */
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Log into the database as this user.
     *
     * @param jdbcUser
     */
    public void setJdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    /**
     * The database user's credentials used to log into the database.
     *
     * @param jdbcPassword
     */
    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * The column in the {@link #setTable(String) table} whose value is the schema version. The value in this column
     * tells you what version the schema is currently at.
     *
     * @param v value column name
     */
    public void setValueColumn(String v) {
        valueColumn = v;
    }

    /**
     * The table name that contains the row where the schema version can be found. This table must have a
     * {@link #setValueColumn(String) value column} whose value is the schema version and it must have a
     * {@link #setKeyColumn(String) key column} whose value matches {@link #setKeyMatch(String)} (which simply
     * identifies the row that contains the schema version value).
     *
     * @param t table name
     */
    public void setTable(String t) {
        tableName = t;
    }

    /**
     * This is the name of the column in the {@link #setTable(String) table} that identifies the row that has the schema
     * version in it. Finding the row where this key column's value is {@link #setKeyMatch(String)} will get you the row
     * whose value in the {@link #setValueColumn(String) value column} is the schema version.
     *
     * @param k key column name
     */
    public void setKeyColumn(String k) {
        keyColumn = k;
    }

    /**
     * This is the value of the {@link #setKeyColumn(String) key column} that identifies the row whose
     * {@link #setValueColumn(String) value column} is the schema version.
     *
     * @param m key value identifying the row that has the schema version
     */
    public void setKeyMatch(String m) {
        keyMatch = m;
    }

    /**
     * A file that contains database type mappings. This is rarely going to be used, since database type mappings never
     * change (unless future databases add new types or change the semantics of their current types).
     *
     * @param f mapping file
     */
    public void setTypeMap(File f) {
        this.typeMapFile = f;
    }

    /**
     * This is the schema version that the schema should be upgraded to.
     *
     * @param v schema version
     */
    public void setTargetSchemaVersion(String v) {
        this.targetSchemaVersionString = v;
    }

    /**
     * Creates a new schema spec object.
     *
     * @return new schema spec
     */
    public SchemaSpec createSchemaSpec() {
        SchemaSpec ss = new SchemaSpec(this);
        schemaSpecs.add(ss);
        return ss;
    }

    /**
     * Returns the collection of database type mappings.
     *
     * @return collection of type mappings
     */
    public Collection<TypeMap> getTypeMaps() {
        return typeMaps;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        List<SchemaSpec> newSpecs = new ArrayList<SchemaSpec>();
        newSpecs.addAll(schemaSpecs);

        // Sort the schema specs - if any reordering occurred, consider that
        // an error.  Also, if there are any duplicate versions, that's an error
        Collections.sort(newSpecs);
        int size = schemaSpecs.size();
        for (int i = 0; i < size; i++) {
            if (!newSpecs.get(i).equals(schemaSpecs.get(i))) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_SCHEMA_SPECS_OUT_OF_ORDER,
                    schemaSpecs.get(i).getVersion()));
            }

            if ((i > 0) && newSpecs.get(i).equals(newSpecs.get(i - 1))) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_DUPLICATE_SCHEMA_SPECS,
                    newSpecs.get(i).getVersion()));
            }
        }

        Connection conn = null;

        try {
            // Connect to the database to grab the starting schema version
            conn = getConnection();
            databaseType = DatabaseTypeFactory.getDatabaseType(conn);

            startSchemaVersionString = loadStartSchemaVersion(conn, databaseType);
            if (startSchemaVersionString.indexOf(SCHEMA_MOD_IN_PROGRESS) != -1) {
                // try to fix this by making the schema version the last successful version
                try {
                    String ver = startSchemaVersionString.substring(0, startSchemaVersionString
                        .indexOf(SCHEMA_MOD_IN_PROGRESS));
                    updateSchemaVersion(conn, databaseType, ver);
                    conn.commit();
                } catch (Exception e) {
                    log(e.toString());
                }

                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_INCONSISTENT_STATE,
                    startSchemaVersionString));
            }

            try {
                startSchemaVersion = new SchemaVersion(startSchemaVersionString);
            } catch (IllegalArgumentException e) {
                throw new BuildException(e.getMessage(), e);
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_STARTING, startSchemaVersion, targetSchemaVersion));

            // If the target version is LATEST, then figure out the "real" target version.
            String realTargetSchemaVersion = targetSchemaVersion.toString();
            if (targetSchemaVersion.getIsLatest()) {
                SchemaSpec latestSpec = schemaSpecs.get(size - 1);
                realTargetSchemaVersion = latestSpec.getVersion().toString();
            }

            // Ensure that we're not trying to "downgrade" - that is,
            // ensure that the target version is not earlier than the
            // existing server version.  In particular, if the target
            // version is LATEST but the actual latest SchemaSpec is
            // earlier than the current database's schema version, we
            // consider that a downgrade as well.
            if (targetSchemaVersion.compareTo(startSchemaVersion) < 0) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_DOWNGRADING,
                    startSchemaVersion, realTargetSchemaVersion));
            }

            size = schemaSpecs.size();
            SchemaSpec ss;
            conn.setAutoCommit(false);
            SchemaVersion fromVersion = startSchemaVersion;
            SchemaVersion toVersion;
            for (int i = 0; i < size; i++) {
                ss = schemaSpecs.get(i);
                toVersion = ss.getVersion();
                if (!shouldExecSpecVersion(toVersion)) {
                    continue;
                }

                log(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_UPGRADE_STEP, fromVersion, toVersion));

                try {
                    markSchemaModificationInProgress(conn, databaseType, fromVersion, toVersion);
                    ss.initialize(conn, this);
                    ss.execute();
                    updateSchemaVersion(conn, databaseType, toVersion.toString());
                    conn.commit();
                    log(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_UPGRADE_STEP_DONE, fromVersion, toVersion));
                    fromVersion = toVersion;
                } catch (Exception e) {
                    try {
                        conn.rollback();
                    } catch (Exception e2) {
                        log("rollback() exception: " + e2.toString());
                    }

                    throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_UPGRADE_STEP_ERROR, ss
                        .getVersion(), e), e);
                }
            }

            // If this was a "upgrade to latest", then ensure that
            // the schema version gets set correctly.
            if (targetSchemaVersion.getIsLatest()) {
                updateSchemaVersion(conn, databaseType, realTargetSchemaVersion);
                conn.commit();
            }

            log(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_SUCCESS, realTargetSchemaVersion));
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            if ((conn != null) && (databaseType != null)) {
                databaseType.closeConnection(conn);
            }
        }
    }

    /**
     * Returns <code>true</code> if the given version's schema spec should be executed.
     *
     * @param  version
     *
     * @return <code>true</code> if this version is on the upgrade path
     */
    protected boolean shouldExecSpecVersion(SchemaVersion version) {
        return version.getIsLatest() || version.between(startSchemaVersion, targetSchemaVersion);
    }

    /**
     * Makes sure the task's attributes are set properly.
     *
     * @throws BuildException
     */
    private void validateAttributes() throws BuildException {
        if (jdbcUrl == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_NO_JDBC_URL));
        }

        if (typeMapFile == null) {
            typeMaps = TypeMap.loadKnownTypeMaps();
        } else {
            try {
                FileInputStream fis = new FileInputStream(typeMapFile);
                typeMaps = TypeMap.loadTypeMapsFromStream(fis);
            } catch (Exception e) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_TYPE_MAP_FILE_ERROR, typeMapFile
                    .getAbsolutePath(), e), e);
            }
        }

        if (targetSchemaVersionString == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_NO_VERSION));
        }

        try {
            targetSchemaVersion = new SchemaVersion(targetSchemaVersionString);
        } catch (IllegalArgumentException e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_INVALID_VERSION, e.getMessage()), e);
        }

        return;
    }

    /**
     * Finding what the current schema version is by looking it up in the version {@link #setTable(String) table}.
     *
     * @param  c
     * @param  db_type
     *
     * @return schema version string as found in the {@link #setKeyColumn(String) key column}.
     *
     * @throws BuildException
     */
    private String loadStartSchemaVersion(Connection c, DatabaseType db_type) throws BuildException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String versionString;
        String sql = "SELECT " + valueColumn + " " + "FROM " + tableName + " " + "WHERE " + keyColumn + " = ? ";

        try {
            ps = c.prepareStatement(sql);
            ps.setString(1, keyMatch);
            rs = ps.executeQuery();
            if (rs.next()) {
                versionString = rs.getString(1);
            } else {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_MISSING_VERSION, tableName,
                    valueColumn, keyColumn));
            }

            if (rs.next()) {
                throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_DUPLICATE_VERSION, tableName,
                    valueColumn, keyColumn));
            }

            return versionString;
        } catch (SQLException e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_LOADING_START_VERSION, e), e);
        } finally {
            db_type.closeStatement(ps);
            db_type.closeResultSet(rs);
        }
    }

    /**
     * Sets the schema version in the {@link #setValueColumn(String) value column} of the
     * {@link #setTable(String) version table} to a value that indicates an upgrade is currently in progress.
     *
     * @param  conn
     * @param  db_type
     * @param  fromVersion
     * @param  toVersion
     *
     * @throws BuildException
     */
    private void markSchemaModificationInProgress(Connection conn, DatabaseType db_type, SchemaVersion fromVersion,
        SchemaVersion toVersion) throws BuildException {
        String versionString = fromVersion.toString() + SCHEMA_MOD_IN_PROGRESS + toVersion.toString();
        updateSchemaVersion(conn, db_type, versionString);
        return;
    }

    /**
     * Sets the schema version in the {@link #setValueColumn(String) value column} of the version
     * {@link #setTable(String)}.
     *
     * @param  conn
     * @param  db_type
     * @param  version
     *
     * @throws BuildException
     */
    private void updateSchemaVersion(Connection conn, DatabaseType db_type, String version) throws BuildException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = "UPDATE " + tableName + " " + "SET " + valueColumn + " = ? " + "WHERE " + keyColumn + " = ? ";

        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, version);
            ps.setString(2, keyMatch);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.DBUPGRADE_ERROR_UPDATING_VERSION, version, e), e);
        } finally {
            db_type.closeStatement(ps);
            db_type.closeResultSet(rs);
        }

        return;
    }

    /**
     * Gets a connection to the database as configured by this task's attributes.
     *
     * @return database connection
     *
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return DbUtil.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    /**
     * Returns the type of database that is being upgraded.
     *
     * @return db type
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
}