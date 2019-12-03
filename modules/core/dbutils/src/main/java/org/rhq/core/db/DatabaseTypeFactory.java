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
package org.rhq.core.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.sql.DataSource;

import mazz.i18n.Logger;

/**
 * A factory class that creates {@link DatabaseType} objects based on the database being connected to. This should be
 * the only class (or at least one of the very few) that actually has database-specific knowledge coded into it.
 */
public class DatabaseTypeFactory {
    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DatabaseTypeFactory.class);

    /**
     * Maps the database type that is associated with a particular connection type.
     */
    private static final Map<String, Class<? extends DatabaseType>> DATABASE_TYPES = new Hashtable<String, Class<? extends DatabaseType>>();

    /**
     * This provides a map that keys JDBC URL protocols with their corresponding <code>java.sql.Driver</code> classes.
     */
    private static final Map<String, String> DB_URL_DRIVER_MAP;

    /**
     * This can be set to store the defaultDatabaseType. Typically set it application initialization to
     * provide a quick way to get the (typically) unchanging db vendor setting.
     */
    private static DatabaseType defaultDatabaseType = null;

    static {
        DB_URL_DRIVER_MAP = new Hashtable<String, String>();
        DB_URL_DRIVER_MAP.put("jdbc:postgresql:", "org.postgresql.Driver");
        DB_URL_DRIVER_MAP.put("jdbc:oracle:thin:@", "oracle.jdbc.driver.OracleDriver");
        DB_URL_DRIVER_MAP.put("jdbc:oracle:oci8:", "oracle.jdbc.driver.OracleDriver");
        DB_URL_DRIVER_MAP.put("jdbc:h2:", "org.h2.Driver");
        DB_URL_DRIVER_MAP.put("jdbc:jtds:sqlserver", "net.sourceforge.jtds.jdbc.Driver");
    }

    /**
     * Prevents instantiation.
     */
    private DatabaseTypeFactory() {
        // don't instantiate us from external
    }

    /**
     * By default, this factory will remember what {@link DatabaseType} is associated with
     * a specific connection class type. However, if you change the JDBC URL, its possible
     * that this will also change the database type the connection class type should be
     * associated with (i.e. the URL may point to a different version of the same vendor's
     * database).  If you change the JDBC URL, you should call this method to clear the
     * cache, thus enabling the factory to re-check the connection metadata to associate
     * a new database type with the connection class type.
     */
    public static void clearDatabaseTypeCache() {
        DATABASE_TYPES.clear();
    }

    /**
     * Convenience method that gives you the {@link DatabaseType} given a context and datasource. This is a combination
     * of {@link #getDatabaseType(Context, String)} and {@link #getDatabaseType(Connection)}.
     *
     * @param  context    the context where the datasource can be found
     * @param  datasource the name of the datasource within the given context
     *
     * @return the {@link DatabaseType} of the type of database behind the data source
     *
     * @throws Exception if cannot find the datasource in the given context or the connection cannot be created or if
     *                   cannot determine what database the connection is connected to
     */
    public static DatabaseType getDatabaseType(Context context, String datasource) throws Exception {
        Connection c = getConnection(context, datasource);

        try {
            return getDatabaseType(c);
        } finally {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Given a context and a JNDI datasource name, this will lookup within that context to find the named datasource. A
     * connection to that datasource will be created and returned - with the intent that the connection will be passed
     * to {@link #getDatabaseType(Connection)} to create a {@link DatabaseType}.
     *
     * @param  context    the context where the datasource can be found
     * @param  datasource the name of the datasource within the given context
     *
     * @return a connection to the given datasource
     *
     * @throws Exception if cannot find the datasource in the given context or the connection cannot be created
     */
    public static Connection getConnection(Context context, String datasource) throws Exception {
        DataSource ds = (DataSource) context.lookup(datasource);

        return ds.getConnection();
    }

    /**
     * Given a <code>Connection</code> object, this method returns its associated {@link DatabaseType}.
     *
     * @param  conn
     *
     * @return the {@link DatabaseType} that the connection is connected to
     *
     * @throws Exception if cannot determine what database the connection is connected to
     */
    public static DatabaseType getDatabaseType(Connection conn) throws Exception {
        String conn_class = conn.getClass().getName();
        Class<? extends DatabaseType> database_type_class = DATABASE_TYPES.get(conn_class);

        if (database_type_class == null) {
            DatabaseMetaData db_metadata = conn.getMetaData();
            String db_name = db_metadata.getDatabaseProductName().toLowerCase();
            String db_version = db_metadata.getDatabaseProductVersion().toLowerCase();

            LOG.debug(DbUtilsI18NResourceKeys.DB_CONNECTION_METADATA, db_name, db_version);

            if (db_name.indexOf("postgresql") != -1) {
                if (db_version.startsWith("7.")) {
                    database_type_class = Postgresql7DatabaseType.class;
                } else if (db_version.startsWith("9.0")) {
                    database_type_class = Postgresql90DatabaseType.class;
                } else if (db_version.startsWith("9.")) {  // 9.1, 9.2, 9.x...
                    database_type_class = Postgresql91DatabaseType.class;
                } else if (db_version.startsWith("8.4")) {
                    database_type_class = Postgresql84DatabaseType.class;
                } else if (db_version.startsWith("8.3")) {
                    database_type_class = Postgresql83DatabaseType.class;
                } else if (db_version.startsWith("8.2")) {
                    database_type_class = Postgresql82DatabaseType.class;
                } else if (db_version.startsWith("8.1")) {
                    database_type_class = Postgresql81DatabaseType.class;
                } else if (db_version.startsWith("8.")) {
                    database_type_class = Postgresql8DatabaseType.class;
                } else {
                    database_type_class = Postgresql91DatabaseType.class;
                }
            } else if (db_name.indexOf("oracle") != -1) {
                if (db_version.startsWith("oracle8")) {
                    database_type_class = Oracle8DatabaseType.class;
                } else if (db_version.startsWith("oracle9")) {
                    database_type_class = Oracle9DatabaseType.class;
                } else if (db_version.startsWith("oracle database 10g")) {
                    database_type_class = Oracle10DatabaseType.class;
                } else if (db_version.startsWith("oracle database 11g")) {
                    database_type_class = Oracle11DatabaseType.class;
                } else if (db_version.startsWith("oracle database 12c")) {
                    database_type_class = Oracle12DatabaseType.class;
                }
            } else if (db_name.indexOf("h2") != -1) {
                if (db_version.startsWith("1.1")) {
                    database_type_class = H2v11DatabaseType.class;
                } else if (db_version.startsWith("1.2")) {
                    database_type_class = H2v12DatabaseType.class;
                }
            } else if (db_name.indexOf("sql server") != -1) {
                if (db_version.startsWith("09.00") || db_version.startsWith("9.00")) { // SQL Server 2005
                    database_type_class = SQLServer2005DatabaseType.class;
                } else if (db_version.startsWith("10.00")) { // SQL Server 2008
                    database_type_class = SQLServer2008DatabaseType.class;
                }
            }

            if (database_type_class == null) {
                throw new Exception(DbUtilsI18NFactory.getMsg().getMsg(DbUtilsI18NResourceKeys.UNKNOWN_DATABASE,
                    db_name, db_version));
            }

            DATABASE_TYPES.put(conn_class, database_type_class);
        }

        return database_type_class.newInstance();
    }

    /**
     * This is the getter of a get/set convenience mechanism for storing and retrieving the active database type.
     * Not a true singleton but typically the value will not change as the underlying db is typically
     * not going to change at runtime.
     *
     * @return The current DatabaseType or null if not yet set.
     */
    public static DatabaseType getDefaultDatabaseType() {
        return DatabaseTypeFactory.defaultDatabaseType;
    }

    /**
     * This is the setter of a get/set convenience mechanism for storing and retrieving the active database type.
     * Typically called one time when the dbType is established.  Not a true singleton but typically the value will
     * not change as the underlying db is typically not going to change.
     */
    public static void setDefaultDatabaseType(DatabaseType databaseType) {
        DatabaseTypeFactory.defaultDatabaseType = databaseType;
    }

    /**
     * Given a JDBC URL, this will attempt to load in the JDBC driver class that will be needed to connect to the
     * database via that URL and will return that driver class name. If the driver class could not be loaded, <code>
     * null</code> will be returned - in which case attempting to connect to the database with the given URL will
     * probably fail.
     *
     * @param  jdbc_url a connection URL to the database
     *
     * @return the driver class that was attempted to have been loaded
     */
    public static String loadJdbcDriver(String jdbc_url) {
        String ret_driver_class = null;

        for (Map.Entry<String, String> url_driver_entry : DB_URL_DRIVER_MAP.entrySet()) {
            String url_prefix = url_driver_entry.getKey();
            if (jdbc_url.startsWith(url_prefix)) {
                String driver_class_to_load = url_driver_entry.getValue();
                try {
                    Class.forName(driver_class_to_load);
                    ret_driver_class = driver_class_to_load;
                } catch (ClassNotFoundException e) {
                    LOG.warn(DbUtilsI18NResourceKeys.CANNOT_LOAD_JDBC_DRIVER, driver_class_to_load, jdbc_url);
                }

                break;
            }
        }

        return ret_driver_class;
    }

    /**
     * Is the database PostgreSQL?
     *
     * @param  c the connection to the database
     *
     * @return <code>true</code> if the connection is talking to a Postgres database
     *
     * @throws Exception
     */
    public static boolean isPostgres(Connection c) throws Exception {
        DatabaseType type = getDatabaseType(c);
        return isPostgres(type);
    }

    /**
     * Determines if the given type refers to a Postgres database.
     *
     * @param  type
     *
     * @return <code>true</code> if the type is a Postgres database
     */
    public static boolean isPostgres(DatabaseType type) {
        return (type instanceof PostgresqlDatabaseType);
    }

    /**
     * Is the database Oracle?
     *
     * @param  c the connection to the database
     *
     * @return <code>true</code> if the connection is talking to an Oracle database
     *
     * @throws Exception
     */
    public static boolean isOracle(Connection c) throws Exception {
        DatabaseType type = getDatabaseType(c);
        return isOracle(type);
    }

    /**
     * Determines if the given type refers to an Oracle database.
     *
     * @param  type
     *
     * @return <code>true</code> if the type is an Oracle database
     */
    public static boolean isOracle(DatabaseType type) {
        return (type instanceof OracleDatabaseType);
    }

    /**
     * Is the database H2?
     *
     * @param  c the connection to the database
     *
     * @return <code>true</code> if the connection is talking to an H2 database
     *
     * @throws Exception
     */
    public static boolean isH2(Connection c) throws Exception {
        DatabaseType type = getDatabaseType(c);
        return isH2(type);
    }

    /**
     * Determines if the given type refers to an H2 database.
     *
     * @param  type
     *
     * @return <code>true</code> if the type is an H2 database
     */
    public static boolean isH2(DatabaseType type) {
        return (type instanceof H2DatabaseType);
    }

    /**
     * Is the database SQL Server?
     *
     * @param  c the connection to the database
     *
     * @return <code>true</code> if the connection is talking to an SQL Server database
     *
     * @throws Exception
     */
    public static boolean isSQLServer(Connection c) throws Exception {
        DatabaseType type = getDatabaseType(c);
        return isSQLServer(type);
    }

    /**
     * Determines if the given type refers to an SQL Server database.
     *
     * @param  type
     *
     * @return <code>true</code> if the type is an SQL Server database
     */
    public static boolean isSQLServer(DatabaseType type) {
        return (type instanceof SQLServerDatabaseType);
    }
}